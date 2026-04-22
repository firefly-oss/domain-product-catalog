/*
 * Copyright 2025 Firefly Software Solutions Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firefly.domain.product.catalog.core.products.simulation.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.firefly.core.product.sdk.api.ProductApi;
import com.firefly.core.product.sdk.api.ProductConfigurationApi;
import com.firefly.core.product.sdk.model.FilterRequestProductConfigurationDTO;
import com.firefly.core.product.sdk.model.PaginationResponse;
import com.firefly.core.product.sdk.model.ProductConfigurationDTO;
import com.firefly.core.product.sdk.model.ProductDTO;
import com.firefly.domain.product.catalog.core.products.simulation.CustomerProfile;
import com.firefly.domain.product.catalog.core.products.simulation.PricingProjection;
import com.firefly.domain.product.catalog.core.products.simulation.ProductSimulationResponse;
import com.firefly.domain.product.catalog.core.products.simulation.ProductSimulationService;
import com.firefly.domain.product.catalog.core.products.simulation.SimulateProductRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.firefly.domain.product.catalog.core.utils.constants.ProductAdminConstants.*;

/**
 * Local simulation engine. Reads pricing and eligibility configs from
 * {@code core-common-product-mgmt} via {@link ProductConfigurationApi} and
 * evaluates them in-process. No downstream pricing service is called.
 *
 * <p>PII rules: only {@code productId}, {@code partyId}, and a per-request
 * correlation id are ever logged. Income, age, and segment are used for
 * eligibility evaluation only — never written to logs.
 */
@Service
@Slf4j
public class ProductSimulationServiceImpl implements ProductSimulationService {

    private static final MathContext MC = new MathContext(12);
    private static final int MONEY_SCALE = 2;
    private static final int RATE_SCALE = 6;

    private final ProductApi productApi;
    private final ProductConfigurationApi productConfigurationApi;
    private final ObjectMapper objectMapper;

    public ProductSimulationServiceImpl(ProductApi productApi,
                                        ProductConfigurationApi productConfigurationApi,
                                        ObjectMapper objectMapper) {
        this.productApi = productApi;
        this.productConfigurationApi = productConfigurationApi;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<ProductSimulationResponse> simulate(UUID productId, SimulateProductRequest req) {
        String requestId = UUID.randomUUID().toString();
        UUID partyId = req.customerProfile() != null ? req.customerProfile().partyId() : null;
        log.info("simulate.start productId={} partyId={} requestId={}", productId, partyId, requestId);

        return productApi.getProductById(productId, null)
                .switchIfEmpty(Mono.defer(() -> Mono.just(new ProductDTO())))
                .flatMap(product -> {
                    if (product.getProductStatus() != ProductDTO.ProductStatusEnum.ACTIVE) {
                        log.info("simulate.ineligible productId={} reason={} requestId={}",
                                productId, REASON_PRODUCT_NOT_ACTIVE, requestId);
                        return Mono.just(ineligible(productId, REASON_PRODUCT_NOT_ACTIVE));
                    }
                    return productConfigurationApi.filterConfigurations(productId,
                                    new FilterRequestProductConfigurationDTO(), null)
                            .map(ProductSimulationServiceImpl::safeContent)
                            .defaultIfEmpty(Collections.emptyList())
                            .map(rawConfigs -> toTyped(rawConfigs))
                            .map(configs -> evaluate(productId, req, configs, requestId));
                });
    }

    // ============================== EVALUATION PIPELINE ==============================

    private ProductSimulationResponse evaluate(UUID productId,
                                               SimulateProductRequest req,
                                               List<ProductConfigurationDTO> configs,
                                               String requestId) {
        List<ProductConfigurationDTO> pricing = configs.stream()
                .filter(ProductSimulationServiceImpl::isPricing)
                .toList();
        List<ProductConfigurationDTO> eligibility = configs.stream()
                .filter(ProductSimulationServiceImpl::isEligibilityRule)
                .toList();

        List<String> reasons = new ArrayList<>();
        for (ProductConfigurationDTO rule : eligibility) {
            List<String> failures = evaluateRule(rule, req.customerProfile(), requestId);
            reasons.addAll(failures);
        }
        if (eligibility.isEmpty()) {
            log.warn("simulate.eligibility-fail-open productId={} reason=no-rules requestId={}",
                    productId, requestId);
        }

        if (pricing.isEmpty()) {
            reasons.add(REASON_NO_PRICING_SCHEME);
            return ineligible(productId, reasons.toArray(new String[0]));
        }

        BigDecimal interestRate = extractInterestRate(pricing.get(0), requestId);
        if (interestRate == null) {
            reasons.add(REASON_NO_PRICING_SCHEME);
            return ineligible(productId, reasons.toArray(new String[0]));
        }

        if (!reasons.isEmpty()) {
            return ineligible(productId, reasons.toArray(new String[0]));
        }

        BigDecimal effectiveApr = extractEffectiveApr(pricing.get(0), interestRate);
        PricingProjection projection = project(interestRate, effectiveApr, req.amount(), req.tenorMonths());
        log.info("simulate.eligible productId={} partyId={} requestId={}",
                productId,
                req.customerProfile() != null ? req.customerProfile().partyId() : null,
                requestId);
        return new ProductSimulationResponse(productId, true, List.of(), projection);
    }

    private List<String> evaluateRule(ProductConfigurationDTO rule, CustomerProfile profile, String requestId) {
        List<String> out = new ArrayList<>();
        JsonNode node = parseJsonSafely(rule.getConfigValue(), rule.getConfigKey(), requestId);
        if (node == null || !node.isObject()) {
            return out; // fail-open per-rule
        }
        String key = rule.getConfigKey() != null ? rule.getConfigKey() : "UNKNOWN_RULE";

        Integer age = profile != null ? profile.age() : null;
        BigDecimal income = profile != null ? profile.income() : null;
        String segment = profile != null ? profile.segment() : null;

        if (node.hasNonNull("minAge") && age != null && age < node.get("minAge").asInt()) {
            out.add(REASON_ELIGIBILITY_RULE_FAILED + ":" + key + ":minAge");
        }
        if (node.hasNonNull("maxAge") && age != null && age > node.get("maxAge").asInt()) {
            out.add(REASON_ELIGIBILITY_RULE_FAILED + ":" + key + ":maxAge");
        }
        if (node.hasNonNull("minIncome") && income != null && income.compareTo(node.get("minIncome").decimalValue()) < 0) {
            out.add(REASON_ELIGIBILITY_RULE_FAILED + ":" + key + ":minIncome");
        }
        if (node.hasNonNull("maxIncome") && income != null && income.compareTo(node.get("maxIncome").decimalValue()) > 0) {
            out.add(REASON_ELIGIBILITY_RULE_FAILED + ":" + key + ":maxIncome");
        }
        if (node.has("allowedSegments") && node.get("allowedSegments").isArray() && segment != null) {
            boolean allowed = false;
            for (JsonNode seg : node.get("allowedSegments")) {
                if (segment.equals(seg.asText())) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) {
                out.add(REASON_ELIGIBILITY_RULE_FAILED + ":" + key + ":segment");
            }
        }
        return out;
    }

    private BigDecimal extractInterestRate(ProductConfigurationDTO pricingConfig, String requestId) {
        JsonNode node = parseJsonSafely(pricingConfig.getConfigValue(), pricingConfig.getConfigKey(), requestId);
        if (node != null && node.hasNonNull("interestRate")) {
            return node.get("interestRate").decimalValue().setScale(RATE_SCALE, RoundingMode.HALF_UP);
        }
        return null;
    }

    private BigDecimal extractEffectiveApr(ProductConfigurationDTO pricingConfig, BigDecimal fallback) {
        JsonNode node = parseJsonSafely(pricingConfig.getConfigValue(), pricingConfig.getConfigKey(), "");
        if (node != null && node.hasNonNull("effectiveApr")) {
            return node.get("effectiveApr").decimalValue().setScale(RATE_SCALE, RoundingMode.HALF_UP);
        }
        return fallback;
    }

    // ============================== AMORTIZATION ==============================

    private static PricingProjection project(BigDecimal interestRate,
                                             BigDecimal effectiveApr,
                                             BigDecimal amount,
                                             Integer tenorMonths) {
        if (tenorMonths == null || tenorMonths <= 0) {
            return new PricingProjection(interestRate, null, null, effectiveApr);
        }
        BigDecimal monthly = amortize(amount, interestRate, tenorMonths);
        BigDecimal total = monthly.multiply(BigDecimal.valueOf(tenorMonths), MC)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        return new PricingProjection(interestRate, monthly, total, effectiveApr);
    }

    private static BigDecimal amortize(BigDecimal principal, BigDecimal annualRate, int tenorMonths) {
        if (annualRate.signum() == 0) {
            return principal.divide(BigDecimal.valueOf(tenorMonths), MONEY_SCALE, RoundingMode.HALF_UP);
        }
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(12), MC);
        // pow(1+r, n) and (1+r)^-n = 1 / pow(1+r, n)
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate, MC);
        BigDecimal factor = onePlusR.pow(tenorMonths, MC);
        BigDecimal denom = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(factor, MC), MC);
        if (denom.signum() == 0) {
            return principal.divide(BigDecimal.valueOf(tenorMonths), MONEY_SCALE, RoundingMode.HALF_UP);
        }
        return principal.multiply(monthlyRate, MC)
                .divide(denom, MC)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    // ============================== HELPERS ==============================

    private static ProductSimulationResponse ineligible(UUID productId, String... reasons) {
        return new ProductSimulationResponse(productId, false, List.of(reasons), null);
    }

    private static List<Object> safeContent(PaginationResponse resp) {
        if (resp == null || resp.getContent() == null) {
            return Collections.emptyList();
        }
        return resp.getContent();
    }

    private List<ProductConfigurationDTO> toTyped(List<Object> raw) {
        return raw.stream()
                .filter(Objects::nonNull)
                .map(c -> objectMapper.convertValue(c, ProductConfigurationDTO.class))
                .toList();
    }

    private static boolean isPricing(ProductConfigurationDTO cfg) {
        return configTypeContains(cfg, CONFIG_TYPE_PRICING) || keyContains(cfg, CONFIG_TYPE_PRICING);
    }

    private static boolean isEligibilityRule(ProductConfigurationDTO cfg) {
        return configTypeContains(cfg, CONFIG_TYPE_ELIGIBILITY_RULE) || keyContains(cfg, CONFIG_TYPE_ELIGIBILITY_RULE);
    }

    private static boolean configTypeContains(ProductConfigurationDTO cfg, String token) {
        return cfg != null && cfg.getConfigType() != null && cfg.getConfigType().getValue() != null
                && cfg.getConfigType().getValue().contains(token);
    }

    private static boolean keyContains(ProductConfigurationDTO cfg, String token) {
        return cfg != null && cfg.getConfigKey() != null && cfg.getConfigKey().contains(token);
    }

    private JsonNode parseJsonSafely(String raw, String key, String requestId) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            log.warn("simulate.config-parse-failed key={} requestId={} err={}", key, requestId, e.getMessage());
            return null;
        }
    }
}

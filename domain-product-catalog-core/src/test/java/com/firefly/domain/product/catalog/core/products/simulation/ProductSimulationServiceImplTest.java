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

package com.firefly.domain.product.catalog.core.products.simulation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.firefly.core.product.sdk.api.ProductApi;
import com.firefly.core.product.sdk.api.ProductConfigurationApi;
import com.firefly.core.product.sdk.model.FilterRequestProductConfigurationDTO;
import com.firefly.core.product.sdk.model.PaginationResponse;
import com.firefly.core.product.sdk.model.ProductConfigurationDTO;
import com.firefly.core.product.sdk.model.ProductDTO;
import com.firefly.domain.product.catalog.core.products.simulation.impl.ProductSimulationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.firefly.domain.product.catalog.core.utils.constants.ProductAdminConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductSimulationServiceImplTest {

    private ProductApi productApi;
    private ProductConfigurationApi productConfigurationApi;
    private ProductSimulationServiceImpl service;

    @BeforeEach
    void setUp() {
        productApi = mock(ProductApi.class);
        productConfigurationApi = mock(ProductConfigurationApi.class);
        service = new ProductSimulationServiceImpl(productApi, productConfigurationApi, new ObjectMapper());
    }

    // ============================== HAPPY PATHS ==============================

    @Test
    void simulate_activeProductWithPricingAndNoRules_returnsEligibleWithProjection() {
        UUID productId = UUID.randomUUID();
        whenProductStatus(productId, ProductDTO.ProductStatusEnum.ACTIVE);
        whenConfigsReturn(productId, pricingConfig("{\"interestRate\": 0.045}"));

        SimulateProductRequest req = new SimulateProductRequest(
                productId,
                new CustomerProfile(UUID.randomUUID(), 35, bd("50000"), "RETAIL"),
                bd("10000"),
                24);

        StepVerifier.create(service.simulate(productId, req))
                .assertNext(resp -> {
                    assertThat(resp.eligible()).isTrue();
                    assertThat(resp.ineligibilityReasons()).isEmpty();
                    assertThat(resp.pricing()).isNotNull();
                    assertThat(resp.pricing().interestRate()).isEqualByComparingTo("0.045");
                    assertThat(resp.pricing().monthlyPayment()).isNotNull();
                    assertThat(resp.pricing().totalCost()).isNotNull();
                    // sanity: monthly*24 ~= totalCost
                    BigDecimal product = resp.pricing().monthlyPayment().multiply(BigDecimal.valueOf(24));
                    assertThat(product.subtract(resp.pricing().totalCost()).abs()).isLessThan(bd("1.00"));
                })
                .verifyComplete();
    }

    @Test
    void simulate_zeroInterestRate_projectsLinearly() {
        UUID productId = UUID.randomUUID();
        whenProductStatus(productId, ProductDTO.ProductStatusEnum.ACTIVE);
        whenConfigsReturn(productId, pricingConfig("{\"interestRate\": 0}"));

        SimulateProductRequest req = new SimulateProductRequest(
                productId,
                new CustomerProfile(UUID.randomUUID(), 30, bd("60000"), "RETAIL"),
                bd("12000"),
                12);

        StepVerifier.create(service.simulate(productId, req))
                .assertNext(resp -> {
                    assertThat(resp.eligible()).isTrue();
                    assertThat(resp.pricing().monthlyPayment()).isEqualByComparingTo("1000.00");
                    assertThat(resp.pricing().totalCost()).isEqualByComparingTo("12000.00");
                })
                .verifyComplete();
    }

    @Test
    void simulate_nullTenor_populatesOnlyRateFields() {
        UUID productId = UUID.randomUUID();
        whenProductStatus(productId, ProductDTO.ProductStatusEnum.ACTIVE);
        whenConfigsReturn(productId, pricingConfig("{\"interestRate\": 0.05, \"effectiveApr\": 0.055}"));

        SimulateProductRequest req = new SimulateProductRequest(
                productId,
                new CustomerProfile(UUID.randomUUID(), 40, bd("80000"), "PREMIUM"),
                bd("5000"),
                null);

        StepVerifier.create(service.simulate(productId, req))
                .assertNext(resp -> {
                    assertThat(resp.eligible()).isTrue();
                    assertThat(resp.pricing().interestRate()).isEqualByComparingTo("0.05");
                    assertThat(resp.pricing().effectiveApr()).isEqualByComparingTo("0.055");
                    assertThat(resp.pricing().monthlyPayment()).isNull();
                    assertThat(resp.pricing().totalCost()).isNull();
                })
                .verifyComplete();
    }

    // ============================== INELIGIBILITY ==============================

    @Test
    void simulate_retiredProduct_returnsIneligibleWithReason() {
        UUID productId = UUID.randomUUID();
        whenProductStatus(productId, ProductDTO.ProductStatusEnum.RETIRED);

        SimulateProductRequest req = new SimulateProductRequest(
                productId,
                new CustomerProfile(UUID.randomUUID(), 35, bd("50000"), "RETAIL"),
                bd("10000"),
                24);

        StepVerifier.create(service.simulate(productId, req))
                .assertNext(resp -> {
                    assertThat(resp.eligible()).isFalse();
                    assertThat(resp.ineligibilityReasons()).containsExactly(REASON_PRODUCT_NOT_ACTIVE);
                    assertThat(resp.pricing()).isNull();
                })
                .verifyComplete();
    }

    @Test
    void simulate_draftProduct_returnsIneligibleWithReason() {
        UUID productId = UUID.randomUUID();
        whenProductStatus(productId, ProductDTO.ProductStatusEnum.DRAFT);

        SimulateProductRequest req = new SimulateProductRequest(productId,
                new CustomerProfile(UUID.randomUUID(), 35, bd("50000"), "RETAIL"),
                bd("10000"), 24);

        StepVerifier.create(service.simulate(productId, req))
                .assertNext(resp -> assertThat(resp.ineligibilityReasons())
                        .containsExactly(REASON_PRODUCT_NOT_ACTIVE))
                .verifyComplete();
    }

    @Test
    void simulate_productMissing_returnsIneligibleProductNotActive() {
        UUID productId = UUID.randomUUID();
        when(productApi.getProductById(eq(productId), any())).thenReturn(Mono.empty());

        SimulateProductRequest req = new SimulateProductRequest(productId,
                new CustomerProfile(UUID.randomUUID(), 30, bd("40000"), "RETAIL"),
                bd("5000"), 12);

        StepVerifier.create(service.simulate(productId, req))
                .assertNext(resp -> {
                    assertThat(resp.eligible()).isFalse();
                    assertThat(resp.ineligibilityReasons()).contains(REASON_PRODUCT_NOT_ACTIVE);
                })
                .verifyComplete();
    }

    @Test
    void simulate_productWithoutPricingConfig_returnsNoPricingScheme() {
        UUID productId = UUID.randomUUID();
        whenProductStatus(productId, ProductDTO.ProductStatusEnum.ACTIVE);
        whenConfigsReturn(productId); // no configs

        SimulateProductRequest req = new SimulateProductRequest(productId,
                new CustomerProfile(UUID.randomUUID(), 30, bd("40000"), "RETAIL"),
                bd("5000"), 12);

        StepVerifier.create(service.simulate(productId, req))
                .assertNext(resp -> {
                    assertThat(resp.eligible()).isFalse();
                    assertThat(resp.ineligibilityReasons()).contains(REASON_NO_PRICING_SCHEME);
                })
                .verifyComplete();
    }

    @Test
    void simulate_pricingJsonMissingInterestRate_returnsNoPricingScheme() {
        UUID productId = UUID.randomUUID();
        whenProductStatus(productId, ProductDTO.ProductStatusEnum.ACTIVE);
        whenConfigsReturn(productId, pricingConfig("{\"currency\": \"EUR\"}"));

        SimulateProductRequest req = new SimulateProductRequest(productId,
                new CustomerProfile(UUID.randomUUID(), 30, bd("40000"), "RETAIL"),
                bd("5000"), 12);

        StepVerifier.create(service.simulate(productId, req))
                .assertNext(resp -> {
                    assertThat(resp.eligible()).isFalse();
                    assertThat(resp.ineligibilityReasons()).contains(REASON_NO_PRICING_SCHEME);
                })
                .verifyComplete();
    }

    // ============================== ELIGIBILITY RULES ==============================

    @Test
    void simulate_customerFailsMinAgeRule_returnsIneligibleWithSpecificReason() {
        UUID productId = UUID.randomUUID();
        whenProductStatus(productId, ProductDTO.ProductStatusEnum.ACTIVE);
        whenConfigsReturn(productId,
                pricingConfig("{\"interestRate\": 0.04}"),
                eligibilityConfig("AGE_BAND", "{\"minAge\": 21, \"maxAge\": 65}"));

        SimulateProductRequest req = new SimulateProductRequest(productId,
                new CustomerProfile(UUID.randomUUID(), 18, bd("50000"), "RETAIL"),
                bd("10000"), 24);

        StepVerifier.create(service.simulate(productId, req))
                .assertNext(resp -> {
                    assertThat(resp.eligible()).isFalse();
                    assertThat(resp.ineligibilityReasons())
                            .anyMatch(r -> r.startsWith(REASON_ELIGIBILITY_RULE_FAILED)
                                    && r.contains("AGE_BAND") && r.endsWith("minAge"));
                })
                .verifyComplete();
    }

    @Test
    void simulate_customerFailsSegmentRule_returnsIneligible() {
        UUID productId = UUID.randomUUID();
        whenProductStatus(productId, ProductDTO.ProductStatusEnum.ACTIVE);
        whenConfigsReturn(productId,
                pricingConfig("{\"interestRate\": 0.04}"),
                eligibilityConfig("SEGMENT", "{\"allowedSegments\": [\"PREMIUM\", \"PRIVATE\"]}"));

        SimulateProductRequest req = new SimulateProductRequest(productId,
                new CustomerProfile(UUID.randomUUID(), 30, bd("40000"), "RETAIL"),
                bd("5000"), 12);

        StepVerifier.create(service.simulate(productId, req))
                .assertNext(resp -> {
                    assertThat(resp.eligible()).isFalse();
                    assertThat(resp.ineligibilityReasons())
                            .anyMatch(r -> r.contains("SEGMENT") && r.endsWith("segment"));
                })
                .verifyComplete();
    }

    @Test
    void simulate_multipleRulesFailed_collectsAllReasons() {
        UUID productId = UUID.randomUUID();
        whenProductStatus(productId, ProductDTO.ProductStatusEnum.ACTIVE);
        whenConfigsReturn(productId,
                pricingConfig("{\"interestRate\": 0.04}"),
                eligibilityConfig("AGE_BAND", "{\"minAge\": 21}"),
                eligibilityConfig("INCOME_BAND", "{\"minIncome\": 30000}"));

        SimulateProductRequest req = new SimulateProductRequest(productId,
                new CustomerProfile(UUID.randomUUID(), 19, bd("10000"), "RETAIL"),
                bd("5000"), 12);

        StepVerifier.create(service.simulate(productId, req))
                .assertNext(resp -> {
                    assertThat(resp.eligible()).isFalse();
                    assertThat(resp.ineligibilityReasons()).hasSize(2);
                    assertThat(resp.ineligibilityReasons())
                            .anyMatch(r -> r.contains("AGE_BAND"))
                            .anyMatch(r -> r.contains("INCOME_BAND"));
                })
                .verifyComplete();
    }

    @Test
    void simulate_corruptRuleJson_failsOpenForThatRule() {
        UUID productId = UUID.randomUUID();
        whenProductStatus(productId, ProductDTO.ProductStatusEnum.ACTIVE);
        whenConfigsReturn(productId,
                pricingConfig("{\"interestRate\": 0.03}"),
                eligibilityConfig("CORRUPT", "not-valid-json"));

        SimulateProductRequest req = new SimulateProductRequest(productId,
                new CustomerProfile(UUID.randomUUID(), 30, bd("50000"), "RETAIL"),
                bd("1000"), 12);

        StepVerifier.create(service.simulate(productId, req))
                .assertNext(resp -> assertThat(resp.eligible()).isTrue())
                .verifyComplete();
    }

    // ============================== HELPERS ==============================

    private void whenProductStatus(UUID productId, ProductDTO.ProductStatusEnum status) {
        ProductDTO product = new ProductDTO(LocalDateTime.now(), LocalDateTime.now(), productId)
                .productCode("TEST")
                .productStatus(status);
        when(productApi.getProductById(eq(productId), any())).thenReturn(Mono.just(product));
    }

    private void whenConfigsReturn(UUID productId, ProductConfigurationDTO... configs) {
        PaginationResponse resp = new PaginationResponse().content(List.of((Object[]) configs));
        when(productConfigurationApi.filterConfigurations(eq(productId),
                any(FilterRequestProductConfigurationDTO.class), any()))
                .thenReturn(Mono.just(resp));
    }

    private static ProductConfigurationDTO pricingConfig(String json) {
        return new ProductConfigurationDTO()
                .configType(ProductConfigurationDTO.ConfigTypeEnum.PRICING)
                .configKey("PRICING_SCHEME")
                .configValue(json);
    }

    private static ProductConfigurationDTO eligibilityConfig(String key, String json) {
        // Uses CUSTOM configType with a configKey that contains ELIGIBILITY_RULE token
        return new ProductConfigurationDTO()
                .configType(ProductConfigurationDTO.ConfigTypeEnum.CUSTOM)
                .configKey("ELIGIBILITY_RULE:" + key)
                .configValue(json);
    }

    private static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }
}

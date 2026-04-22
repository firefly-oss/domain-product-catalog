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

package com.firefly.domain.product.catalog.core.products.versions.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.firefly.core.product.sdk.api.ProductConfigurationApi;
import com.firefly.core.product.sdk.api.ProductVersionApi;
import com.firefly.core.product.sdk.model.FilterRequestProductConfigurationDTO;
import com.firefly.core.product.sdk.model.FilterRequestProductVersionDTO;
import com.firefly.core.product.sdk.model.PaginationRequest;
import com.firefly.core.product.sdk.model.PaginationResponse;
import com.firefly.core.product.sdk.model.ProductConfigurationDTO;
import com.firefly.core.product.sdk.model.ProductVersionDTO;
import com.firefly.domain.product.catalog.core.products.versions.FieldDiff;
import com.firefly.domain.product.catalog.core.products.versions.ProductVersionService;
import com.firefly.domain.product.catalog.core.products.versions.ProductVersionSummary;
import com.firefly.domain.product.catalog.core.products.versions.VersionComparisonResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class ProductVersionServiceImpl implements ProductVersionService {

    private static final int VERSION_PAGE_SIZE = 200;

    private final ProductVersionApi productVersionApi;
    private final ProductConfigurationApi productConfigurationApi;
    private final ObjectMapper objectMapper;

    public ProductVersionServiceImpl(ProductVersionApi productVersionApi,
                                     ProductConfigurationApi productConfigurationApi,
                                     ObjectMapper objectMapper) {
        this.productVersionApi = productVersionApi;
        this.productConfigurationApi = productConfigurationApi;
        this.objectMapper = objectMapper;
    }

    // ============================== LIST VERSIONS ==============================

    @Override
    public Mono<List<ProductVersionSummary>> listVersions(UUID productId) {
        FilterRequestProductVersionDTO filter = new FilterRequestProductVersionDTO()
                .pagination(new PaginationRequest().pageNumber(0).pageSize(VERSION_PAGE_SIZE));
        return productVersionApi.filterProductVersions(productId, filter, null)
                .map(ProductVersionServiceImpl::safeContent)
                .defaultIfEmpty(Collections.emptyList())
                .map(raw -> raw.stream()
                        .filter(Objects::nonNull)
                        .map(v -> objectMapper.convertValue(v, ProductVersionDTO.class))
                        .map(ProductVersionServiceImpl::toSummary)
                        .toList());
    }

    // ============================== COMPARE VERSIONS ==============================

    @Override
    public Mono<VersionComparisonResponse> compareVersions(UUID productId, UUID versionIdA, UUID versionIdB) {
        Mono<JsonNode> snapshotA = versionSnapshot(productId, versionIdA);
        Mono<JsonNode> snapshotB = versionSnapshot(productId, versionIdB);
        return Mono.zip(snapshotA, snapshotB)
                .map(tuple -> diffAndBuild(productId, versionIdA, versionIdB, tuple.getT1(), tuple.getT2()));
    }

    private Mono<JsonNode> versionSnapshot(UUID productId, UUID versionId) {
        Mono<ProductVersionDTO> versionMono = productVersionApi.getProductVersionById(productId, versionId, null)
                .switchIfEmpty(Mono.just(new ProductVersionDTO()));
        Mono<List<ProductConfigurationDTO>> configs = productConfigurationApi
                .filterConfigurations(productId, new FilterRequestProductConfigurationDTO(), null)
                .map(ProductVersionServiceImpl::safeContent)
                .defaultIfEmpty(Collections.emptyList())
                .map(raw -> raw.stream()
                        .filter(Objects::nonNull)
                        .map(c -> objectMapper.convertValue(c, ProductConfigurationDTO.class))
                        .toList());

        return Mono.zip(versionMono, configs)
                .map(tuple -> {
                    // Snapshot only the business-meaningful fields — ids and audit
                    // timestamps always differ between two versions of the same product
                    // and would swamp the diff with noise.
                    Map<String, Object> versionFields = new LinkedHashMap<>();
                    versionFields.put("versionNumber", tuple.getT1().getVersionNumber());
                    versionFields.put("versionDescription", tuple.getT1().getVersionDescription());
                    versionFields.put("effectiveDate", tuple.getT1().getEffectiveDate());

                    List<Map<String, Object>> configurationFields = new ArrayList<>();
                    for (ProductConfigurationDTO cfg : tuple.getT2()) {
                        Map<String, Object> c = new LinkedHashMap<>();
                        c.put("configType", cfg.getConfigType() != null ? cfg.getConfigType().getValue() : null);
                        c.put("configKey", cfg.getConfigKey());
                        c.put("configValue", cfg.getConfigValue());
                        configurationFields.add(c);
                    }

                    Map<String, Object> snapshot = new LinkedHashMap<>();
                    snapshot.put("version", versionFields);
                    snapshot.put("configurations", configurationFields);
                    return objectMapper.valueToTree(snapshot);
                });
    }

    private VersionComparisonResponse diffAndBuild(UUID productId,
                                                   UUID versionIdA,
                                                   UUID versionIdB,
                                                   JsonNode snapshotA,
                                                   JsonNode snapshotB) {
        List<FieldDiff> added = new ArrayList<>();
        List<FieldDiff> removed = new ArrayList<>();
        List<FieldDiff> changed = new ArrayList<>();
        diffRecursive("$", snapshotA, snapshotB, added, removed, changed);
        return new VersionComparisonResponse(productId, versionIdA, versionIdB, added, removed, changed);
    }

    // ============================== NAIVE JSON DIFF ==============================

    private static void diffRecursive(String path,
                                      JsonNode a,
                                      JsonNode b,
                                      List<FieldDiff> added,
                                      List<FieldDiff> removed,
                                      List<FieldDiff> changed) {
        if (a == null || a.isMissingNode() || a.isNull()) {
            if (b != null && !b.isMissingNode() && !b.isNull()) {
                added.add(new FieldDiff(path, null, textOf(b)));
            }
            return;
        }
        if (b == null || b.isMissingNode() || b.isNull()) {
            removed.add(new FieldDiff(path, textOf(a), null));
            return;
        }
        if (a.isObject() && b.isObject()) {
            Set<String> keys = new HashSet<>();
            Iterator<String> ka = a.fieldNames();
            while (ka.hasNext()) {
                keys.add(ka.next());
            }
            Iterator<String> kb = b.fieldNames();
            while (kb.hasNext()) {
                keys.add(kb.next());
            }
            for (String key : keys) {
                diffRecursive(path + "." + key, a.get(key), b.get(key), added, removed, changed);
            }
            return;
        }
        if (a.isArray() && b.isArray()) {
            int size = Math.max(a.size(), b.size());
            for (int i = 0; i < size; i++) {
                JsonNode itemA = i < a.size() ? a.get(i) : null;
                JsonNode itemB = i < b.size() ? b.get(i) : null;
                diffRecursive(path + "[" + i + "]", itemA, itemB, added, removed, changed);
            }
            return;
        }
        if (!a.equals(b)) {
            changed.add(new FieldDiff(path, textOf(a), textOf(b)));
        }
    }

    // ============================== HELPERS ==============================

    private static List<Object> safeContent(PaginationResponse resp) {
        if (resp == null || resp.getContent() == null) {
            return Collections.emptyList();
        }
        return resp.getContent();
    }

    private static ProductVersionSummary toSummary(ProductVersionDTO v) {
        return new ProductVersionSummary(
                v.getProductVersionId(),
                v.getVersionNumber(),
                v.getVersionDescription(),
                v.getEffectiveDate(),
                v.getDateCreated());
    }

    private static String textOf(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return node.isValueNode() ? node.asText() : node.toString();
    }
}

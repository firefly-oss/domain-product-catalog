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

package com.firefly.domain.product.catalog.core.products.versions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.firefly.core.product.sdk.api.ProductConfigurationApi;
import com.firefly.core.product.sdk.api.ProductVersionApi;
import com.firefly.core.product.sdk.model.FilterRequestProductConfigurationDTO;
import com.firefly.core.product.sdk.model.FilterRequestProductVersionDTO;
import com.firefly.core.product.sdk.model.PaginationResponse;
import com.firefly.core.product.sdk.model.ProductConfigurationDTO;
import com.firefly.core.product.sdk.model.ProductVersionDTO;
import com.firefly.domain.product.catalog.core.products.versions.impl.ProductVersionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductVersionServiceImplTest {

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 4, 1, 12, 0);

    private ProductVersionApi productVersionApi;
    private ProductConfigurationApi productConfigurationApi;
    private ObjectMapper objectMapper;
    private ProductVersionServiceImpl service;

    @BeforeEach
    void setUp() {
        productVersionApi = mock(ProductVersionApi.class);
        productConfigurationApi = mock(ProductConfigurationApi.class);
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        service = new ProductVersionServiceImpl(productVersionApi, productConfigurationApi, objectMapper);
    }

    // ============================== LIST VERSIONS ==============================

    @Test
    void listVersions_returnsMappedSummaries() {
        UUID productId = UUID.randomUUID();
        UUID v1Id = UUID.randomUUID();
        UUID v2Id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        ProductVersionDTO v1 = new ProductVersionDTO(now, now, v1Id)
                .productId(productId).versionNumber(1L).versionDescription("initial").effectiveDate(now);
        ProductVersionDTO v2 = new ProductVersionDTO(now, now, v2Id)
                .productId(productId).versionNumber(2L).versionDescription("update").effectiveDate(now);

        when(productVersionApi.filterProductVersions(eq(productId), any(FilterRequestProductVersionDTO.class), any()))
                .thenReturn(Mono.just(new PaginationResponse().content(List.of(v1, v2))));

        StepVerifier.create(service.listVersions(productId))
                .assertNext(list -> {
                    assertThat(list).hasSize(2);
                    assertThat(list).extracting(ProductVersionSummary::versionNumber).containsExactly(1L, 2L);
                    assertThat(list).extracting(ProductVersionSummary::versionDescription)
                            .containsExactly("initial", "update");
                })
                .verifyComplete();
    }

    @Test
    void listVersions_emptyResponse_returnsEmptyList() {
        UUID productId = UUID.randomUUID();
        when(productVersionApi.filterProductVersions(eq(productId), any(), any()))
                .thenReturn(Mono.just(new PaginationResponse().content(List.of())));

        StepVerifier.create(service.listVersions(productId))
                .assertNext(list -> assertThat(list).isEmpty())
                .verifyComplete();
    }

    // ============================== COMPARE VERSIONS ==============================

    @Test
    void compareVersions_identicalSnapshots_returnsEmptyDiffs() {
        UUID productId = UUID.randomUUID();
        UUID vA = UUID.randomUUID();
        UUID vB = UUID.randomUUID();
        mockVersion(productId, vA, 1L, "initial");
        mockVersion(productId, vB, 1L, "initial");
        mockConfigs(productId, List.of());

        StepVerifier.create(service.compareVersions(productId, vA, vB))
                .assertNext(resp -> {
                    assertThat(resp.productId()).isEqualTo(productId);
                    assertThat(resp.added()).isEmpty();
                    assertThat(resp.removed()).isEmpty();
                    assertThat(resp.changed()).isEmpty();
                })
                .verifyComplete();
    }

    @Test
    void compareVersions_differingVersionNumber_reportedAsChanged() {
        UUID productId = UUID.randomUUID();
        UUID vA = UUID.randomUUID();
        UUID vB = UUID.randomUUID();
        mockVersion(productId, vA, 1L, "initial");
        mockVersion(productId, vB, 2L, "initial");
        mockConfigs(productId, List.of());

        StepVerifier.create(service.compareVersions(productId, vA, vB))
                .assertNext(resp -> {
                    assertThat(resp.changed())
                            .anyMatch(d -> d.path().contains("versionNumber")
                                    && "1".equals(d.oldValue())
                                    && "2".equals(d.newValue()));
                })
                .verifyComplete();
    }

    @Test
    void compareVersions_descriptionOnlyInB_isAddedButNull_so_noDiff() {
        UUID productId = UUID.randomUUID();
        UUID vA = UUID.randomUUID();
        UUID vB = UUID.randomUUID();
        mockVersion(productId, vA, 1L, null);
        mockVersion(productId, vB, 1L, "new desc");
        mockConfigs(productId, List.of());

        StepVerifier.create(service.compareVersions(productId, vA, vB))
                .assertNext(resp -> {
                    assertThat(resp.added())
                            .anyMatch(d -> d.path().contains("versionDescription")
                                    && "new desc".equals(d.newValue()));
                })
                .verifyComplete();
    }

    @Test
    void compareVersions_configurationDiff_reportedByPath() {
        UUID productId = UUID.randomUUID();
        UUID vA = UUID.randomUUID();
        UUID vB = UUID.randomUUID();
        mockVersion(productId, vA, 1L, "stable");
        mockVersion(productId, vB, 1L, "stable");

        ProductConfigurationDTO config = new ProductConfigurationDTO()
                .configType(ProductConfigurationDTO.ConfigTypeEnum.PRICING)
                .configKey("RATE").configValue("0.04");
        mockConfigs(productId, List.of(config));

        StepVerifier.create(service.compareVersions(productId, vA, vB))
                .assertNext(resp -> {
                    // Snapshots identical -> no diffs
                    assertThat(resp.changed()).isEmpty();
                    assertThat(resp.added()).isEmpty();
                    assertThat(resp.removed()).isEmpty();
                })
                .verifyComplete();
    }

    // ============================== HELPERS ==============================

    private void mockVersion(UUID productId, UUID versionId, Long versionNumber, String description) {
        ProductVersionDTO dto = new ProductVersionDTO(FIXED_NOW, FIXED_NOW, versionId)
                .productId(productId)
                .versionNumber(versionNumber)
                .versionDescription(description)
                .effectiveDate(FIXED_NOW);
        when(productVersionApi.getProductVersionById(eq(productId), eq(versionId), any()))
                .thenReturn(Mono.just(dto));
    }

    private void mockConfigs(UUID productId, List<ProductConfigurationDTO> configs) {
        when(productConfigurationApi.filterConfigurations(eq(productId),
                any(FilterRequestProductConfigurationDTO.class), any()))
                .thenReturn(Mono.just(new PaginationResponse().content(List.of((Object[]) configs.toArray()))));
    }
}

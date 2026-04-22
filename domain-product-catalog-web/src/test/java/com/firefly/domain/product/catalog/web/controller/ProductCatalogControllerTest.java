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

package com.firefly.domain.product.catalog.web.controller;

import com.firefly.domain.product.catalog.core.products.commands.CloneProductCommand;
import com.firefly.domain.product.catalog.core.products.commands.RetireWithMigrationCommand;
import com.firefly.domain.product.catalog.core.products.services.ProductCatalogService;
import com.firefly.domain.product.catalog.core.products.simulation.CustomerProfile;
import com.firefly.domain.product.catalog.core.products.simulation.ProductSimulationResponse;
import com.firefly.domain.product.catalog.core.products.simulation.SimulateProductRequest;
import com.firefly.domain.product.catalog.core.products.tree.CatalogTreeResponse;
import com.firefly.domain.product.catalog.core.products.versions.FieldDiff;
import com.firefly.domain.product.catalog.core.products.versions.ProductVersionSummary;
import com.firefly.domain.product.catalog.core.products.versions.VersionComparisonResponse;
import com.firefly.domain.product.catalog.interfaces.rest.CloneProductRequest;
import com.firefly.domain.product.catalog.interfaces.rest.RetireWithMigrationRequest;
import org.fireflyframework.orchestration.saga.engine.SagaResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductCatalogControllerTest {

    private ProductCatalogService service;
    private ProductCatalogController controller;

    @BeforeEach
    void setUp() {
        service = mock(ProductCatalogService.class);
        controller = new ProductCatalogController(service);
    }

    @Test
    void cloneProduct_mapsRequestToCommandAndDelegates() {
        UUID productId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        CloneProductRequest req = new CloneProductRequest("NEW-CODE", tenantId);

        ArgumentCaptor<CloneProductCommand> captor = ArgumentCaptor.forClass(CloneProductCommand.class);
        when(service.cloneProduct(captor.capture())).thenReturn(Mono.empty());

        StepVerifier.create(controller.cloneProduct(productId, req))
                .assertNext(resp -> assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue())
                .verifyComplete();

        CloneProductCommand sent = captor.getValue();
        assertThat(sent.getSourceProductId()).isEqualTo(productId);
        assertThat(sent.getNewProductCode()).isEqualTo("NEW-CODE");
        assertThat(sent.getTenantId()).isEqualTo(tenantId);
    }

    @Test
    void simulateProduct_forwardsBothPathAndBody() {
        UUID productId = UUID.randomUUID();
        SimulateProductRequest req = new SimulateProductRequest(productId,
                new CustomerProfile(UUID.randomUUID(), 30, new BigDecimal("40000"), "RETAIL"),
                new BigDecimal("5000"), 12);
        ProductSimulationResponse resp = new ProductSimulationResponse(productId, true, List.of(), null);
        when(service.simulateProduct(eq(productId), any(SimulateProductRequest.class))).thenReturn(Mono.just(resp));

        StepVerifier.create(controller.simulateProduct(productId, req))
                .assertNext(entity -> {
                    assertThat(entity.getStatusCode().is2xxSuccessful()).isTrue();
                    assertThat(entity.getBody()).isEqualTo(resp);
                })
                .verifyComplete();
    }

    @Test
    void retireWithMigration_mapsRequestToCommandAndDelegates() {
        UUID productId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        RetireWithMigrationRequest req = new RetireWithMigrationRequest(
                targetId, LocalDate.of(2026, 12, 31), "End of life");

        ArgumentCaptor<RetireWithMigrationCommand> captor = ArgumentCaptor.forClass(RetireWithMigrationCommand.class);
        when(service.retireWithMigration(captor.capture())).thenReturn(Mono.empty());

        StepVerifier.create(controller.retireWithMigration(productId, req))
                .assertNext(resp -> assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue())
                .verifyComplete();

        RetireWithMigrationCommand sent = captor.getValue();
        assertThat(sent.getSourceProductId()).isEqualTo(productId);
        assertThat(sent.getTargetProductId()).isEqualTo(targetId);
        assertThat(sent.getGracePeriodEndDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(sent.getReason()).isEqualTo("End of life");
    }

    @Test
    void getCatalogTree_forwardsTenantIdAndReturnsResponse() {
        UUID tenantId = UUID.randomUUID();
        CatalogTreeResponse resp = new CatalogTreeResponse(tenantId, List.of());
        when(service.getCatalogTree(eq(tenantId))).thenReturn(Mono.just(resp));

        StepVerifier.create(controller.getCatalogTree(tenantId))
                .assertNext(entity -> {
                    assertThat(entity.getStatusCode().is2xxSuccessful()).isTrue();
                    assertThat(entity.getBody()).isEqualTo(resp);
                })
                .verifyComplete();
    }

    @Test
    void listVersions_returnsServiceResult() {
        UUID productId = UUID.randomUUID();
        List<ProductVersionSummary> summaries = List.of();
        when(service.listVersions(eq(productId))).thenReturn(Mono.just(summaries));

        StepVerifier.create(controller.listVersions(productId))
                .assertNext(entity -> {
                    assertThat(entity.getStatusCode().is2xxSuccessful()).isTrue();
                    assertThat(entity.getBody()).isSameAs(summaries);
                })
                .verifyComplete();
    }

    @Test
    void compareVersions_forwardsBothVersionIds() {
        UUID productId = UUID.randomUUID();
        UUID vA = UUID.randomUUID();
        UUID vB = UUID.randomUUID();
        VersionComparisonResponse resp = new VersionComparisonResponse(productId, vA, vB,
                List.of(), List.of(), List.of(new FieldDiff("$.x", "1", "2")));
        when(service.compareVersions(eq(productId), eq(vA), eq(vB))).thenReturn(Mono.just(resp));

        StepVerifier.create(controller.compareVersions(productId, vA, vB))
                .assertNext(entity -> {
                    assertThat(entity.getStatusCode().is2xxSuccessful()).isTrue();
                    assertThat(entity.getBody()).isEqualTo(resp);
                })
                .verifyComplete();
    }

    @Test
    void cloneProduct_propagatesSagaResultCompletion() {
        SagaResult result = mock(SagaResult.class);
        when(service.cloneProduct(any())).thenReturn(Mono.just(result));

        StepVerifier.create(controller.cloneProduct(UUID.randomUUID(),
                        new CloneProductRequest("X", null)))
                .assertNext(resp -> assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue())
                .verifyComplete();
    }
}

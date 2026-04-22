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

package com.firefly.domain.product.catalog.core.products.tree;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.firefly.core.product.sdk.api.ProductApi;
import com.firefly.core.product.sdk.api.ProductCategoryApi;
import com.firefly.core.product.sdk.model.FilterRequestProductCategoryDTO;
import com.firefly.core.product.sdk.model.FilterRequestProductDTO;
import com.firefly.core.product.sdk.model.PaginationResponse;
import com.firefly.core.product.sdk.model.ProductCategoryDTO;
import com.firefly.core.product.sdk.model.ProductDTO;
import com.firefly.domain.product.catalog.core.products.tree.impl.ProductCatalogTreeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductCatalogTreeServiceImplTest {

    private ProductApi productApi;
    private ProductCategoryApi productCategoryApi;
    private ProductCatalogTreeServiceImpl service;

    @BeforeEach
    void setUp() {
        productApi = mock(ProductApi.class);
        productCategoryApi = mock(ProductCategoryApi.class);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        service = new ProductCatalogTreeServiceImpl(productApi, productCategoryApi, objectMapper);
    }

    @Test
    void getCatalogTree_emptyCategories_returnsEmptyTree() {
        when(productCategoryApi.filterCategories(any(FilterRequestProductCategoryDTO.class), any()))
                .thenReturn(Mono.just(new PaginationResponse().content(List.of())));

        StepVerifier.create(service.getCatalogTree(UUID.randomUUID()))
                .assertNext(tree -> assertThat(tree.categories()).isEmpty())
                .verifyComplete();
    }

    @Test
    void getCatalogTree_singleRootWithProducts_buildsLeafNode() {
        UUID tenantId = UUID.randomUUID();
        UUID rootId = UUID.randomUUID();
        ProductCategoryDTO root = category(rootId, null, "Loans", 0);

        when(productCategoryApi.filterCategories(any(), any()))
                .thenReturn(Mono.just(new PaginationResponse().content(List.of(root))));

        UUID productId = UUID.randomUUID();
        ProductDTO product = new ProductDTO(LocalDateTime.now(), LocalDateTime.now(), productId)
                .productCode("LOAN-001")
                .productName("Personal Loan")
                .productStatus(ProductDTO.ProductStatusEnum.ACTIVE);
        when(productApi.filterProducts(any(FilterRequestProductDTO.class), any()))
                .thenReturn(Mono.just(new PaginationResponse().content(List.of(product))));

        StepVerifier.create(service.getCatalogTree(tenantId))
                .assertNext(tree -> {
                    assertThat(tree.tenantId()).isEqualTo(tenantId);
                    assertThat(tree.categories()).hasSize(1);
                    CategoryNode node = tree.categories().get(0);
                    assertThat(node.categoryId()).isEqualTo(rootId);
                    assertThat(node.name()).isEqualTo("Loans");
                    assertThat(node.subcategories()).isEmpty();
                    assertThat(node.products()).hasSize(1);
                    assertThat(node.products().get(0).productCode()).isEqualTo("LOAN-001");
                    assertThat(node.products().get(0).productStatus()).isEqualTo("ACTIVE");
                })
                .verifyComplete();
    }

    @Test
    void getCatalogTree_parentWithChildren_nestsCorrectly() {
        UUID rootId = UUID.randomUUID();
        UUID childA = UUID.randomUUID();
        UUID childB = UUID.randomUUID();

        List<ProductCategoryDTO> cats = List.of(
                category(rootId, null, "Loans", 0),
                category(childA, rootId, "Personal", 1),
                category(childB, rootId, "Mortgage", 1));

        when(productCategoryApi.filterCategories(any(), any()))
                .thenReturn(Mono.just(new PaginationResponse().content(cats.stream().map(c -> (Object) c).toList())));
        when(productApi.filterProducts(any(), any()))
                .thenReturn(Mono.just(new PaginationResponse().content(List.of())));

        StepVerifier.create(service.getCatalogTree(UUID.randomUUID()))
                .assertNext(tree -> {
                    assertThat(tree.categories()).hasSize(1);
                    CategoryNode root = tree.categories().get(0);
                    assertThat(root.subcategories()).hasSize(2);
                    assertThat(root.subcategories())
                            .extracting(CategoryNode::name)
                            .containsExactlyInAnyOrder("Personal", "Mortgage");
                })
                .verifyComplete();
    }

    @Test
    void getCatalogTree_cycleBetweenCategories_breaksAtRevisit() {
        UUID aId = UUID.randomUUID();
        UUID bId = UUID.randomUUID();

        // A -> B (parent = A), B -> A (parent = B) = cycle
        List<ProductCategoryDTO> cats = List.of(
                categoryWithParent(aId, bId, "A", 0),
                categoryWithParent(bId, aId, "B", 0));

        when(productCategoryApi.filterCategories(any(), any()))
                .thenReturn(Mono.just(new PaginationResponse().content(cats.stream().map(c -> (Object) c).toList())));
        when(productApi.filterProducts(any(), any()))
                .thenReturn(Mono.just(new PaginationResponse().content(List.of())));

        // Cycle detection does not blow up — tree returns with the cycle broken.
        StepVerifier.create(service.getCatalogTree(UUID.randomUUID()))
                .assertNext(tree -> assertThat(tree.categories()).isNotNull())
                .verifyComplete();
    }

    @Test
    void getCatalogTree_orphanedCategory_treatedAsRoot() {
        UUID orphanId = UUID.randomUUID();
        UUID phantomParent = UUID.randomUUID();
        // Parent is not in the category list — this category is rendered as a root
        ProductCategoryDTO orphan = category(orphanId, phantomParent, "Orphan", 2);

        when(productCategoryApi.filterCategories(any(), any()))
                .thenReturn(Mono.just(new PaginationResponse().content(List.of(orphan))));
        when(productApi.filterProducts(any(), any()))
                .thenReturn(Mono.just(new PaginationResponse().content(List.of())));

        StepVerifier.create(service.getCatalogTree(UUID.randomUUID()))
                .assertNext(tree -> {
                    assertThat(tree.categories()).hasSize(1);
                    assertThat(tree.categories().get(0).categoryId()).isEqualTo(orphanId);
                })
                .verifyComplete();
    }

    // ============================== HELPERS ==============================

    private static ProductCategoryDTO category(UUID id, UUID parent, String name, Integer level) {
        return new ProductCategoryDTO(LocalDateTime.now(), LocalDateTime.now(), id, level)
                .categoryName(name)
                .parentCategoryId(parent);
    }

    private static ProductCategoryDTO categoryWithParent(UUID id, UUID parent, String name, Integer level) {
        return category(id, parent, name, level);
    }
}

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

package com.firefly.domain.product.catalog.core.products.tree.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.firefly.core.product.sdk.api.ProductApi;
import com.firefly.core.product.sdk.api.ProductCategoryApi;
import com.firefly.core.product.sdk.model.FilterRequestProductCategoryDTO;
import com.firefly.core.product.sdk.model.FilterRequestProductDTO;
import com.firefly.core.product.sdk.model.PaginationRequest;
import com.firefly.core.product.sdk.model.PaginationResponse;
import com.firefly.core.product.sdk.model.ProductCategoryDTO;
import com.firefly.core.product.sdk.model.ProductDTO;
import com.firefly.domain.product.catalog.core.products.tree.CatalogTreeResponse;
import com.firefly.domain.product.catalog.core.products.tree.CategoryNode;
import com.firefly.domain.product.catalog.core.products.tree.ProductCatalogTreeService;
import com.firefly.domain.product.catalog.core.products.tree.ProductSummaryInTree;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Builds a nested {@link CatalogTreeResponse} by pulling all categories and
 * their products from {@code core-common-product-mgmt} and linking them via
 * {@code parentCategoryId}.
 *
 * <p>A maximum depth of {@value #MAX_DEPTH} is enforced so data-model cycles
 * (a parent referencing a descendant) cannot cause infinite recursion. A
 * {@code seen} set logs and breaks on the first revisited node.
 */
@Service
@Slf4j
public class ProductCatalogTreeServiceImpl implements ProductCatalogTreeService {

    private static final int CATEGORY_PAGE_SIZE = 500;
    private static final int MAX_DEPTH = 5;

    private final ProductApi productApi;
    private final ProductCategoryApi productCategoryApi;
    private final ObjectMapper objectMapper;

    public ProductCatalogTreeServiceImpl(ProductApi productApi,
                                         ProductCategoryApi productCategoryApi,
                                         ObjectMapper objectMapper) {
        this.productApi = productApi;
        this.productCategoryApi = productCategoryApi;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<CatalogTreeResponse> getCatalogTree(UUID tenantId) {
        FilterRequestProductCategoryDTO categoryFilter = new FilterRequestProductCategoryDTO()
                .pagination(new PaginationRequest().pageNumber(0).pageSize(CATEGORY_PAGE_SIZE));

        return productCategoryApi.filterCategories(categoryFilter, null)
                .map(ProductCatalogTreeServiceImpl::safeContent)
                .defaultIfEmpty(Collections.emptyList())
                .flatMap(rawCategories -> {
                    List<ProductCategoryDTO> categories = rawCategories.stream()
                            .filter(Objects::nonNull)
                            .map(c -> objectMapper.convertValue(c, ProductCategoryDTO.class))
                            .toList();

                    return Flux.fromIterable(categories)
                            .flatMap(cat -> productsForCategory(cat.getProductCategoryId(), tenantId)
                                    .map(products -> Map.entry(cat.getProductCategoryId(), products)))
                            .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                            .map(productsByCategory -> buildTree(tenantId, categories, productsByCategory));
                });
    }

    private Mono<List<ProductSummaryInTree>> productsForCategory(UUID categoryId, UUID tenantId) {
        ProductDTO filter = new ProductDTO()
                .productCategoryId(categoryId);
        if (tenantId != null) {
            filter.setTenantId(tenantId);
        }
        FilterRequestProductDTO req = new FilterRequestProductDTO().filters(filter);
        return productApi.filterProducts(req, null)
                .map(ProductCatalogTreeServiceImpl::safeContent)
                .defaultIfEmpty(Collections.emptyList())
                .map(rawProducts -> rawProducts.stream()
                        .filter(Objects::nonNull)
                        .map(p -> objectMapper.convertValue(p, ProductDTO.class))
                        .map(ProductCatalogTreeServiceImpl::toSummary)
                        .toList());
    }

    // ============================== TREE BUILDING ==============================

    private CatalogTreeResponse buildTree(UUID tenantId,
                                          List<ProductCategoryDTO> categories,
                                          Map<UUID, List<ProductSummaryInTree>> productsByCategory) {
        Map<UUID, ProductCategoryDTO> byId = new HashMap<>();
        Map<UUID, List<ProductCategoryDTO>> childrenByParent = new LinkedHashMap<>();
        List<ProductCategoryDTO> roots = new ArrayList<>();

        for (ProductCategoryDTO cat : categories) {
            if (cat.getProductCategoryId() == null) {
                continue;
            }
            byId.put(cat.getProductCategoryId(), cat);
            if (cat.getParentCategoryId() == null || !containsCategory(categories, cat.getParentCategoryId())) {
                roots.add(cat);
            } else {
                childrenByParent
                        .computeIfAbsent(cat.getParentCategoryId(), k -> new ArrayList<>())
                        .add(cat);
            }
        }

        List<CategoryNode> rootNodes = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        for (ProductCategoryDTO root : roots) {
            rootNodes.add(toNode(root, childrenByParent, productsByCategory, seen, 0));
        }
        return new CatalogTreeResponse(tenantId, rootNodes);
    }

    private CategoryNode toNode(ProductCategoryDTO cat,
                                Map<UUID, List<ProductCategoryDTO>> childrenByParent,
                                Map<UUID, List<ProductSummaryInTree>> productsByCategory,
                                Set<UUID> seen,
                                int depth) {
        if (!seen.add(cat.getProductCategoryId())) {
            log.warn("catalog-tree.cycle-detected categoryId={}", cat.getProductCategoryId());
            return new CategoryNode(cat.getProductCategoryId(), cat.getCategoryName(), cat.getLevel(),
                    List.of(), List.of());
        }
        List<CategoryNode> children = new ArrayList<>();
        if (depth < MAX_DEPTH) {
            List<ProductCategoryDTO> rawChildren = childrenByParent.getOrDefault(
                    cat.getProductCategoryId(), Collections.emptyList());
            for (ProductCategoryDTO child : rawChildren) {
                children.add(toNode(child, childrenByParent, productsByCategory, seen, depth + 1));
            }
        } else if (childrenByParent.containsKey(cat.getProductCategoryId())) {
            log.warn("catalog-tree.max-depth-reached categoryId={} depth={}", cat.getProductCategoryId(), depth);
        }
        List<ProductSummaryInTree> products = productsByCategory.getOrDefault(
                cat.getProductCategoryId(), Collections.emptyList());
        return new CategoryNode(cat.getProductCategoryId(), cat.getCategoryName(), cat.getLevel(), children, products);
    }

    // ============================== HELPERS ==============================

    private static boolean containsCategory(List<ProductCategoryDTO> categories, UUID parentId) {
        for (ProductCategoryDTO c : categories) {
            if (parentId.equals(c.getProductCategoryId())) {
                return true;
            }
        }
        return false;
    }

    private static List<Object> safeContent(PaginationResponse resp) {
        if (resp == null || resp.getContent() == null) {
            return Collections.emptyList();
        }
        return resp.getContent();
    }

    private static ProductSummaryInTree toSummary(ProductDTO product) {
        return new ProductSummaryInTree(
                product.getProductId(),
                product.getProductCode(),
                product.getProductName(),
                product.getProductStatus() != null ? product.getProductStatus().getValue() : null);
    }
}

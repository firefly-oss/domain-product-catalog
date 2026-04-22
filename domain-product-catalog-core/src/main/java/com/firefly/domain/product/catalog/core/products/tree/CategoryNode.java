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

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/**
 * Recursive catalog tree node — a category with its nested subcategories and
 * the products attached to it at the current level.
 */
@Schema(description = "Recursive catalog tree node.")
public record CategoryNode(
        @Schema(description = "Category identifier.") UUID categoryId,
        @Schema(description = "Category display name.") String name,
        @Schema(description = "Hierarchy level as returned by the core service.") Integer level,
        @Schema(description = "Nested subcategories. Empty when this is a leaf.")
        List<CategoryNode> subcategories,
        @Schema(description = "Products attached at this category level.")
        List<ProductSummaryInTree> products
) {}

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
 * Top-level catalog tree response — collects all root categories (those with no
 * parent) along with their nested children and products for a given tenant.
 */
@Schema(description = "Catalog tree: roots plus nested categories and products.")
public record CatalogTreeResponse(
        @Schema(description = "Tenant identifier the tree was scoped to (echo of the input).")
        UUID tenantId,
        @Schema(description = "Top-level categories. Each carries its own subcategories and products.")
        List<CategoryNode> categories
) {}

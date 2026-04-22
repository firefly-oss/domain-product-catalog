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

import java.util.UUID;

/**
 * Compact product representation embedded inside a {@link CategoryNode}.
 */
@Schema(description = "Compact product descriptor embedded in a catalog tree node.")
public record ProductSummaryInTree(
        @Schema(description = "Product identifier.") UUID productId,
        @Schema(description = "Business-facing product code.") String productCode,
        @Schema(description = "Display name.") String productName,
        @Schema(description = "Lifecycle status, e.g. ACTIVE / DRAFT / RETIRED.") String productStatus
) {}

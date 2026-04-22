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

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/**
 * Result of comparing two product versions: added, removed and changed fields
 * at JSON-path granularity.
 */
@Schema(description = "Comparison of two product versions.")
public record VersionComparisonResponse(
        @Schema(description = "Product identifier both versions belong to.") UUID productId,
        @Schema(description = "Identifier of the first version compared.") UUID versionIdA,
        @Schema(description = "Identifier of the second version compared.") UUID versionIdB,
        @Schema(description = "Fields present in version B but not in version A.") List<FieldDiff> added,
        @Schema(description = "Fields present in version A but not in version B.") List<FieldDiff> removed,
        @Schema(description = "Fields present in both versions with different values.") List<FieldDiff> changed
) {}

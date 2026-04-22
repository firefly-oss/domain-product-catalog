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

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * Read-only service for listing product versions and producing naive field-by-
 * field diffs between any two versions of the same product.
 */
public interface ProductVersionService {

    /**
     * List all versions for a product.
     */
    Mono<List<ProductVersionSummary>> listVersions(UUID productId);

    /**
     * Compare two versions of the same product using a naive JSON-path based
     * diff. Configuration snapshots are included on a best-effort basis.
     */
    Mono<VersionComparisonResponse> compareVersions(UUID productId, UUID versionIdA, UUID versionIdB);
}

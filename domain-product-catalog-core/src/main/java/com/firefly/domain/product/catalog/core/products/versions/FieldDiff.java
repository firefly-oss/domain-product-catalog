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

/**
 * Single field difference inside a version comparison. Values are serialized to
 * their string form; consumers that need structured payloads must re-parse.
 */
@Schema(description = "Field-level diff inside a version comparison.")
public record FieldDiff(
        @Schema(description = "JSON-path to the differing field.") String path,
        @Schema(description = "Value from the first version (null for added fields).") String oldValue,
        @Schema(description = "Value from the second version (null for removed fields).") String newValue
) {}

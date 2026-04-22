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

package com.firefly.domain.product.catalog.core.products.simulation;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Minimal subset of customer attributes consumed by the product simulation
 * engine. Fields MUST NOT be written to logs — they are used only for local
 * eligibility evaluation and discarded after the response is produced.
 */
@Schema(description = "Customer attributes consumed by product simulation (never logged).")
public record CustomerProfile(
        @Schema(description = "Party identifier (UUID).") UUID partyId,
        @Schema(description = "Customer age in years.") Integer age,
        @Schema(description = "Annual declared income.") BigDecimal income,
        @Schema(description = "Segmentation code, e.g. RETAIL / PREMIUM / SME.") String segment
) {}

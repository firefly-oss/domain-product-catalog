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

import java.util.List;
import java.util.UUID;

/**
 * Outcome of a product simulation: eligibility decision with a collected list of
 * reasons (if ineligible) and a pricing projection (if eligible and a pricing
 * scheme exists).
 */
@Schema(description = "Result of a product simulation.")
public record ProductSimulationResponse(
        @Schema(description = "Product identifier the simulation was run for.")
        UUID productId,
        @Schema(description = "Whether the customer is eligible for the product.")
        boolean eligible,
        @Schema(description = "Ordered list of reasons when not eligible; empty when eligible.")
        List<String> ineligibilityReasons,
        @Schema(description = "Pricing projection when eligible; null otherwise.")
        PricingProjection pricing
) {}

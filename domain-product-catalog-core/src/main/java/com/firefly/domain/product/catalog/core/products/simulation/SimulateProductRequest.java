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
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * What-if simulation input: ask the catalog service to project eligibility and
 * pricing for a given product and customer profile without creating any
 * persistent artifacts.
 *
 * <p>Pricing and eligibility rules are read from the product's existing
 * configurations in {@code core-common-product-mgmt} — no separate pricing
 * service is consulted.
 */
@Schema(description = "What-if simulation input for a product.")
public record SimulateProductRequest(
        @Schema(description = "Product identifier (echo of path variable; both must match).")
        UUID productId,
        @Valid @NotNull
        @Schema(description = "Customer profile used for eligibility evaluation.")
        CustomerProfile customerProfile,
        @NotNull @Positive
        @Schema(description = "Principal amount requested.")
        BigDecimal amount,
        @Positive
        @Schema(description = "Loan/subscription tenor in months. Optional — omit for non-amortized simulations.")
        Integer tenorMonths
) {}

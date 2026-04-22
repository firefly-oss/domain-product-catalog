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

import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Computes product eligibility and a pricing projection for a given customer
 * profile without persisting any state or calling a separate pricing service.
 * Pricing rules and eligibility filters are stored in the product's existing
 * configurations on {@code core-common-product-mgmt}.
 */
public interface ProductSimulationService {

    /**
     * Project eligibility and pricing for {@code productId} given a customer
     * profile.
     *
     * @param productId the target product (must match {@code req.productId()})
     * @param req simulation input
     * @return a response describing eligibility + pricing; never an error
     *         signal (missing data yields structured ineligibility reasons).
     */
    Mono<ProductSimulationResponse> simulate(UUID productId, SimulateProductRequest req);
}

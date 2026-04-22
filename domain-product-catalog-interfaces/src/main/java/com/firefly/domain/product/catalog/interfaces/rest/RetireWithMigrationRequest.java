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

package com.firefly.domain.product.catalog.interfaces.rest;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

/**
 * HTTP request body for retiring a product with a migration pointer to an
 * active replacement. The source product id is carried in the URL path.
 */
@Schema(description = "Request body for POST /api/v1/products/{productId}/retire-with-migration.")
public record RetireWithMigrationRequest(
        @NotNull
        @Schema(description = "Identifier of the active target product to migrate contracts toward.")
        UUID targetProductId,
        @Schema(description = "Date after which outstanding contracts on the source product should be migrated.")
        LocalDate gracePeriodEndDate,
        @Schema(description = "Human-readable reason for retiring the source product (audit trail).")
        String reason
) {}

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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * HTTP request body for cloning an existing product. The source product id is
 * carried in the URL path; the body supplies the new code and an optional tenant
 * override.
 */
@Schema(description = "Request body for POST /api/v1/products/{productId}/clone.")
public record CloneProductRequest(
        @NotBlank @Size(min = 1, max = 100)
        @Schema(description = "Product code for the clone — must be unique within the tenant.")
        String newProductCode,
        @Schema(description = "Optional tenant override. When absent, the clone inherits the source product's tenant.")
        UUID tenantId
) {}

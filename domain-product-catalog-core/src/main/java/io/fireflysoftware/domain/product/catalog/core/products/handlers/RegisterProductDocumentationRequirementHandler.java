/*
 * Copyright 2025 Firefly Software Foundation
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


package io.fireflysoftware.domain.product.catalog.core.products.handlers;

import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import io.fireflysoftware.core.product.sdk.api.ProductDocumentationRequirementsApi;
import io.fireflysoftware.domain.product.catalog.core.products.commands.RegisterProductDocumentationRequirementCommand;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class RegisterProductDocumentationRequirementHandler extends CommandHandler<RegisterProductDocumentationRequirementCommand, UUID> {

    private final ProductDocumentationRequirementsApi productDocumentationRequirementsApi;

    public RegisterProductDocumentationRequirementHandler(ProductDocumentationRequirementsApi productDocumentationRequirementsApi) {
        this.productDocumentationRequirementsApi = productDocumentationRequirementsApi;
    }

    @Override
    protected Mono<UUID> doHandle(RegisterProductDocumentationRequirementCommand cmd) {
        return productDocumentationRequirementsApi.createDocumentationRequirement(cmd.getProductId(), cmd, UUID.randomUUID().toString())
                .mapNotNull(productDocumentationDTO ->
                        Objects.requireNonNull(Objects.requireNonNull(productDocumentationDTO)).getProductDocRequirementId());
    }
}
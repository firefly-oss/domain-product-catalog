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
import io.fireflysoftware.core.product.sdk.api.ProductRelationshipApi;
import io.fireflysoftware.domain.product.catalog.core.products.commands.RegisterProductRelationshipCommand;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class RegisterProductRelationshipHandler extends CommandHandler<RegisterProductRelationshipCommand, UUID> {

    private final ProductRelationshipApi productRelationshipApi;

    public RegisterProductRelationshipHandler(ProductRelationshipApi productRelationshipApi) {
        this.productRelationshipApi = productRelationshipApi;
    }

    @Override
    protected Mono<UUID> doHandle(RegisterProductRelationshipCommand cmd) {
        return productRelationshipApi.createRelationship(cmd.getProductId(), cmd, UUID.randomUUID().toString())
                .mapNotNull(relationshipDTO ->
                        Objects.requireNonNull(Objects.requireNonNull(relationshipDTO)).getProductRelationshipId());
    }
}
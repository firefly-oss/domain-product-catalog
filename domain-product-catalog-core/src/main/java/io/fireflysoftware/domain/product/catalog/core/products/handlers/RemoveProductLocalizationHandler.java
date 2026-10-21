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
import io.fireflysoftware.core.product.sdk.api.ProductLocalizationApi;
import io.fireflysoftware.domain.product.catalog.core.products.commands.RemoveProductLocalizationCommand;
import reactor.core.publisher.Mono;
import java.util.UUID;

@CommandHandlerComponent
public class RemoveProductLocalizationHandler extends CommandHandler<RemoveProductLocalizationCommand, Void> {

    private final ProductLocalizationApi productLocalizationApi;

    public RemoveProductLocalizationHandler(ProductLocalizationApi productLocalizationApi) {
        this.productLocalizationApi = productLocalizationApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveProductLocalizationCommand cmd) {
        return productLocalizationApi.deleteLocalization(cmd.productId(), cmd.productLocalizationId(), UUID.randomUUID().toString()).then();
    }
}
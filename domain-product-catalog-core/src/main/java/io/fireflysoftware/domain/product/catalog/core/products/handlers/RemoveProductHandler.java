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
import io.fireflysoftware.core.product.sdk.api.ProductApi;
import io.fireflysoftware.core.product.sdk.model.ProductDTO;
import io.fireflysoftware.domain.product.catalog.core.products.commands.RemoveProductCommand;
import reactor.core.publisher.Mono;
import java.util.UUID;

@CommandHandlerComponent
public class RemoveProductHandler extends CommandHandler<RemoveProductCommand, Void> {

    private final ProductApi productApi;

    public RemoveProductHandler(ProductApi productApi) {
        this.productApi = productApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveProductCommand cmd) {
        return productApi.getProductById(cmd.productId(), UUID.randomUUID().toString())
                .flatMap(product -> {
                    ProductDTO.ProductStatusEnum status = product.getProductStatus();
                    if (status != ProductDTO.ProductStatusEnum.DRAFT
                            && status != ProductDTO.ProductStatusEnum.PROPOSED) {
                        return Mono.error(new IllegalStateException(
                                "Product " + cmd.productId() + " is " + status
                                        + " and cannot be deleted; retire it so agreements that reference it keep a resolvable definition"));
                    }
                    return productApi.deleteProduct(cmd.productId(), UUID.randomUUID().toString());
                })
                .then();
    }
}
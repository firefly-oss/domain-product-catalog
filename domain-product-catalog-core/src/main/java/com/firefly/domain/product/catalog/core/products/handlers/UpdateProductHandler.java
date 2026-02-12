package com.firefly.domain.product.catalog.core.products.handlers;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.command.CommandHandler;
import com.firefly.core.product.sdk.api.ProductApi;
import com.firefly.domain.product.catalog.core.products.commands.UpdateProductInfoCommand;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class UpdateProductHandler extends CommandHandler<UpdateProductInfoCommand, UUID> {

    private final ProductApi productApi;

    public UpdateProductHandler(ProductApi productApi) {
        this.productApi = productApi;
    }

    @Override
    protected Mono<UUID> doHandle(UpdateProductInfoCommand cmd) {
        return productApi.updateProduct(cmd.getProductId(), cmd, UUID.randomUUID().toString())
                .mapNotNull(productDTO ->
                        Objects.requireNonNull(Objects.requireNonNull(productDTO).getProductId()));
    }
}
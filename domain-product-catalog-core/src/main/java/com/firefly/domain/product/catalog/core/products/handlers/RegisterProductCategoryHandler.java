package com.firefly.domain.product.catalog.core.products.handlers;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.command.CommandHandler;
import com.firefly.common.product.sdk.api.ProductCategoryApi;
import com.firefly.domain.product.catalog.core.products.commands.RegisterProductCategoryCommand;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class RegisterProductCategoryHandler extends CommandHandler<RegisterProductCategoryCommand, UUID> {

    private final ProductCategoryApi productCategoryApi;

    public RegisterProductCategoryHandler(ProductCategoryApi productCategoryApi) {
        this.productCategoryApi = productCategoryApi;
    }

    @Override
    protected Mono<UUID> doHandle(RegisterProductCategoryCommand cmd) {
        return productCategoryApi.createCategory(cmd, UUID.randomUUID().toString())
                .mapNotNull(naturalPersonDTO ->
                        Objects.requireNonNull(Objects.requireNonNull(naturalPersonDTO)).getProductCategoryId());
    }
}
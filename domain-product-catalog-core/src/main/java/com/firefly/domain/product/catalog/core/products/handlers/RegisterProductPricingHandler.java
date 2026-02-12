package com.firefly.domain.product.catalog.core.products.handlers;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.command.CommandHandler;
import com.firefly.core.product.sdk.api.ProductPricingApi;
import com.firefly.domain.product.catalog.core.products.commands.RegisterProductPricingCommand;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class RegisterProductPricingHandler extends CommandHandler<RegisterProductPricingCommand, UUID> {

    private final ProductPricingApi productPricingApi;

    public RegisterProductPricingHandler(ProductPricingApi productPricingApi) {
        this.productPricingApi = productPricingApi;
    }

    @Override
    protected Mono<UUID> doHandle(RegisterProductPricingCommand cmd) {
        return productPricingApi.createPricing(cmd.getProductId(), cmd, UUID.randomUUID().toString())
                .mapNotNull(productPricingDTO ->
                        Objects.requireNonNull(Objects.requireNonNull(productPricingDTO)).getProductPricingId());
    }
}
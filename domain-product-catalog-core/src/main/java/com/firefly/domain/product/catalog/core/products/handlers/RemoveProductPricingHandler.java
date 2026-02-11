package com.firefly.domain.product.catalog.core.products.handlers;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.command.CommandHandler;
import com.firefly.common.product.sdk.api.ProductPricingApi;
import com.firefly.domain.product.catalog.core.products.commands.RemoveProductPricingCommand;
import reactor.core.publisher.Mono;

@CommandHandlerComponent
public class RemoveProductPricingHandler extends CommandHandler<RemoveProductPricingCommand, Void> {

    private final ProductPricingApi productPricingApi;

    public RemoveProductPricingHandler(ProductPricingApi productPricingApi) {
        this.productPricingApi = productPricingApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveProductPricingCommand cmd) {
        return productPricingApi.deletePricing(cmd.productId(), cmd.productPricingId()).then();
    }
}
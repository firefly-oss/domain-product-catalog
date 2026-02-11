package com.firefly.domain.product.catalog.core.products.handlers;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.command.CommandHandler;
import com.firefly.common.product.sdk.api.ProductPricingLocalizationApi;
import com.firefly.domain.product.catalog.core.products.commands.RemoveProductPricingLocalizationCommand;
import reactor.core.publisher.Mono;

@CommandHandlerComponent
public class RemoveProductPricingLocalizationHandler extends CommandHandler<RemoveProductPricingLocalizationCommand, Void> {

    private final ProductPricingLocalizationApi productPricingLocalizationApi;

    public RemoveProductPricingLocalizationHandler(ProductPricingLocalizationApi productPricingLocalizationApi) {
        this.productPricingLocalizationApi = productPricingLocalizationApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveProductPricingLocalizationCommand cmd) {
        return productPricingLocalizationApi.deleteLocalization(cmd.productId(), cmd.productPricingId(), cmd.productPricingLocalizationId()).then();
    }
}
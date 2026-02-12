package com.firefly.domain.product.catalog.core.products.handlers;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.command.CommandHandler;
import com.firefly.core.product.sdk.api.ProductFeatureApi;
import com.firefly.domain.product.catalog.core.products.commands.RemoveProductFeatureCommand;
import reactor.core.publisher.Mono;

@CommandHandlerComponent
public class RemoveProductFeatureHandler extends CommandHandler<RemoveProductFeatureCommand, Void> {

    private final ProductFeatureApi productFeatureApi;

    public RemoveProductFeatureHandler(ProductFeatureApi productFeatureApi) {
        this.productFeatureApi = productFeatureApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveProductFeatureCommand cmd) {
        return productFeatureApi.deleteFeature(cmd.productId(), cmd.productFeatureId()).then();
    }
}
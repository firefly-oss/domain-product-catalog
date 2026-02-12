package com.firefly.domain.product.catalog.core.products.handlers;

import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import com.firefly.core.product.sdk.api.ProductConfigurationApi;
import com.firefly.domain.product.catalog.core.products.commands.RemoveProductFeatureCommand;
import reactor.core.publisher.Mono;

@CommandHandlerComponent
public class RemoveProductFeatureHandler extends CommandHandler<RemoveProductFeatureCommand, Void> {

    private final ProductConfigurationApi productConfigurationApi;

    public RemoveProductFeatureHandler(ProductConfigurationApi productConfigurationApi) {
        this.productConfigurationApi = productConfigurationApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveProductFeatureCommand cmd) {
        return productConfigurationApi.deleteConfiguration(cmd.productId(), cmd.productFeatureId(), null).then();
    }
}

package com.firefly.domain.product.catalog.core.products.handlers;

import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import com.firefly.core.product.sdk.api.ProductConfigurationApi;
import com.firefly.domain.product.catalog.core.products.commands.RemoveProductPricingCommand;
import reactor.core.publisher.Mono;

@CommandHandlerComponent
public class RemoveProductPricingHandler extends CommandHandler<RemoveProductPricingCommand, Void> {

    private final ProductConfigurationApi productConfigurationApi;

    public RemoveProductPricingHandler(ProductConfigurationApi productConfigurationApi) {
        this.productConfigurationApi = productConfigurationApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveProductPricingCommand cmd) {
        return productConfigurationApi.deleteConfiguration(cmd.productId(), cmd.productPricingId(), null).then();
    }
}

package com.firefly.domain.product.catalog.core.products.handlers;

import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import com.firefly.core.product.sdk.api.ProductConfigurationApi;
import com.firefly.domain.product.catalog.core.products.commands.RemoveProductLimitCommand;
import reactor.core.publisher.Mono;

@CommandHandlerComponent
public class RemoveProductLimitHandler extends CommandHandler<RemoveProductLimitCommand, Void> {

    private final ProductConfigurationApi productConfigurationApi;

    public RemoveProductLimitHandler(ProductConfigurationApi productConfigurationApi) {
        this.productConfigurationApi = productConfigurationApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveProductLimitCommand cmd) {
        return productConfigurationApi.deleteConfiguration(cmd.productId(), cmd.productLimitId(), null).then();
    }
}

package com.firefly.domain.product.catalog.core.products.handlers;

import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import com.firefly.core.product.sdk.api.ProductConfigurationApi;
import com.firefly.domain.product.catalog.core.products.commands.RemoveProductLifecycleCommand;
import reactor.core.publisher.Mono;

@CommandHandlerComponent
public class RemoveProductLifecycleHandler extends CommandHandler<RemoveProductLifecycleCommand, Void> {

    private final ProductConfigurationApi productConfigurationApi;

    public RemoveProductLifecycleHandler(ProductConfigurationApi productConfigurationApi) {
        this.productConfigurationApi = productConfigurationApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveProductLifecycleCommand cmd) {
        return productConfigurationApi.deleteConfiguration(cmd.productId(), cmd.productLifecycleId(), null).then();
    }
}

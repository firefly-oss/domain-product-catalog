package com.firefly.domain.product.catalog.core.products.handlers;

import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import com.firefly.core.product.sdk.api.ProductConfigurationApi;
import com.firefly.domain.product.catalog.core.products.commands.RemoveFeeComponentCommand;
import reactor.core.publisher.Mono;

@CommandHandlerComponent
public class RemoveFeeComponentHandler extends CommandHandler<RemoveFeeComponentCommand, Void> {

    private final ProductConfigurationApi productConfigurationApi;

    public RemoveFeeComponentHandler(ProductConfigurationApi productConfigurationApi) {
        this.productConfigurationApi = productConfigurationApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveFeeComponentCommand cmd) {
        return productConfigurationApi.deleteConfiguration(cmd.productId(), cmd.feeComponentId(), null).then();
    }
}

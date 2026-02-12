package com.firefly.domain.product.catalog.core.products.handlers;

import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import com.firefly.core.product.sdk.api.ProductConfigurationApi;
import com.firefly.domain.product.catalog.core.products.commands.RemoveProductSubtypeCommand;
import reactor.core.publisher.Mono;

@CommandHandlerComponent
public class RemoveProductSubtypeHandler extends CommandHandler<RemoveProductSubtypeCommand, Void> {

    private final ProductConfigurationApi productConfigurationApi;

    public RemoveProductSubtypeHandler(ProductConfigurationApi productConfigurationApi) {
        this.productConfigurationApi = productConfigurationApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveProductSubtypeCommand cmd) {
        return productConfigurationApi.deleteConfiguration(cmd.productId(), cmd.productSubtypeId(), null).then();
    }
}

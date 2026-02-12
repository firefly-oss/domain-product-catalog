package com.firefly.domain.product.catalog.core.products.handlers;

import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import com.firefly.core.product.sdk.api.ProductConfigurationApi;
import com.firefly.domain.product.catalog.core.products.commands.RemoveProductBundleCommand;
import reactor.core.publisher.Mono;

@CommandHandlerComponent
public class RemoveProductBundleHandler extends CommandHandler<RemoveProductBundleCommand, Void> {

    private final ProductConfigurationApi productConfigurationApi;

    public RemoveProductBundleHandler(ProductConfigurationApi productConfigurationApi) {
        this.productConfigurationApi = productConfigurationApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveProductBundleCommand cmd) {
        return productConfigurationApi.deleteConfiguration(cmd.productId(), cmd.productBundleId(), null).then();
    }
}

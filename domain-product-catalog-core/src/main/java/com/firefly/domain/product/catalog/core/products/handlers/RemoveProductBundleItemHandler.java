package com.firefly.domain.product.catalog.core.products.handlers;

import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import com.firefly.core.product.sdk.api.ProductConfigurationApi;
import com.firefly.domain.product.catalog.core.products.commands.RemoveProductBundleItemCommand;
import reactor.core.publisher.Mono;

@CommandHandlerComponent
public class RemoveProductBundleItemHandler extends CommandHandler<RemoveProductBundleItemCommand, Void> {

    private final ProductConfigurationApi productConfigurationApi;

    public RemoveProductBundleItemHandler(ProductConfigurationApi productConfigurationApi) {
        this.productConfigurationApi = productConfigurationApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveProductBundleItemCommand cmd) {
        return productConfigurationApi.deleteConfiguration(cmd.productId(), cmd.productBundleItemId(), null).then();
    }
}

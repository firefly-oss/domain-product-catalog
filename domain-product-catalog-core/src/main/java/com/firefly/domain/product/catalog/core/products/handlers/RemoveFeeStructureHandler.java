package com.firefly.domain.product.catalog.core.products.handlers;

import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import com.firefly.core.product.sdk.api.ProductConfigurationApi;
import com.firefly.domain.product.catalog.core.products.commands.RemoveFeeStructureCommand;
import reactor.core.publisher.Mono;

@CommandHandlerComponent
public class RemoveFeeStructureHandler extends CommandHandler<RemoveFeeStructureCommand, Void> {

    private final ProductConfigurationApi productConfigurationApi;

    public RemoveFeeStructureHandler(ProductConfigurationApi productConfigurationApi) {
        this.productConfigurationApi = productConfigurationApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveFeeStructureCommand cmd) {
        return productConfigurationApi.deleteConfiguration(cmd.productId(), cmd.feeStructureId(), null).then();
    }
}

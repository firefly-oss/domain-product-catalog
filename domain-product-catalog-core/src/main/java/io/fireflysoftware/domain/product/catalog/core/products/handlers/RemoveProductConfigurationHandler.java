package io.fireflysoftware.domain.product.catalog.core.products.handlers;

import io.fireflysoftware.core.product.sdk.api.ProductConfigurationApi;
import io.fireflysoftware.domain.product.catalog.core.products.commands.RemoveProductConfigurationCommand;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import reactor.core.publisher.Mono;

import java.util.UUID;

@CommandHandlerComponent
public class RemoveProductConfigurationHandler extends CommandHandler<RemoveProductConfigurationCommand, Void> {

    private final ProductConfigurationApi productConfigurationApi;

    public RemoveProductConfigurationHandler(ProductConfigurationApi productConfigurationApi) {
        this.productConfigurationApi = productConfigurationApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveProductConfigurationCommand cmd) {
        return productConfigurationApi.deleteConfiguration(cmd.productId(), cmd.configurationId(), UUID.randomUUID().toString()).then();
    }
}

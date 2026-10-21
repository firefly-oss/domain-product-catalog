package io.fireflysoftware.domain.product.catalog.core.products.handlers;

import io.fireflysoftware.core.product.sdk.api.ProductConfigurationApi;
import io.fireflysoftware.domain.product.catalog.core.products.commands.RegisterProductConfigurationCommand;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class RegisterProductConfigurationHandler extends CommandHandler<RegisterProductConfigurationCommand, UUID> {

    private final ProductConfigurationApi productConfigurationApi;

    public RegisterProductConfigurationHandler(ProductConfigurationApi productConfigurationApi) {
        this.productConfigurationApi = productConfigurationApi;
    }

    @Override
    protected Mono<UUID> doHandle(RegisterProductConfigurationCommand cmd) {
        return productConfigurationApi.createConfiguration(cmd.getProductId(), cmd, UUID.randomUUID().toString())
                .mapNotNull(dto -> Objects.requireNonNull(Objects.requireNonNull(dto).getProductConfigurationId()));
    }
}

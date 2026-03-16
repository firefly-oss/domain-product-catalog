package com.firefly.domain.product.catalog.core.products.handlers;

import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import com.firefly.core.product.sdk.api.ProductConfigurationApi;
import com.firefly.domain.product.catalog.core.products.commands.RegisterProductBundleCommand;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class RegisterProductBundleHandler extends CommandHandler<RegisterProductBundleCommand, UUID> {

    private final ProductConfigurationApi productConfigurationApi;

    public RegisterProductBundleHandler(ProductConfigurationApi productConfigurationApi) {
        this.productConfigurationApi = productConfigurationApi;
    }

    @Override
    protected Mono<UUID> doHandle(RegisterProductBundleCommand cmd) {
        return productConfigurationApi.createConfiguration(cmd.getProductId(), cmd, UUID.randomUUID().toString())
                .mapNotNull(productConfigurationDTO ->
                        Objects.requireNonNull(Objects.requireNonNull(productConfigurationDTO)).getProductConfigurationId());
    }
}

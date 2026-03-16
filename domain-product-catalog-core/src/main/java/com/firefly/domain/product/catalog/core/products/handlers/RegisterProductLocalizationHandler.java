package com.firefly.domain.product.catalog.core.products.handlers;

import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import com.firefly.core.product.sdk.api.ProductLocalizationApi;
import com.firefly.domain.product.catalog.core.products.commands.RegisterProductLocalizationCommand;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class RegisterProductLocalizationHandler extends CommandHandler<RegisterProductLocalizationCommand, UUID> {

    private final ProductLocalizationApi productLocalizationApi;

    public RegisterProductLocalizationHandler(ProductLocalizationApi productLocalizationApi) {
        this.productLocalizationApi = productLocalizationApi;
    }

    @Override
    protected Mono<UUID> doHandle(RegisterProductLocalizationCommand cmd) {
        return productLocalizationApi.createLocalization(cmd.getProductId(), cmd, UUID.randomUUID().toString())
                .mapNotNull(productLocalizationDTO ->
                        Objects.requireNonNull(Objects.requireNonNull(productLocalizationDTO)).getProductLocalizationId());
    }
}
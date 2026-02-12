package com.firefly.domain.product.catalog.core.products.handlers;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.command.CommandHandler;
import com.firefly.core.product.sdk.api.ProductPricingLocalizationApi;
import com.firefly.domain.product.catalog.core.products.commands.RegisterProductPricingLocalizationCommand;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class RegisterProductPricingLocalizationHandler extends CommandHandler<RegisterProductPricingLocalizationCommand, UUID> {

    private final ProductPricingLocalizationApi productPricingLocalizationApi;

    public RegisterProductPricingLocalizationHandler(ProductPricingLocalizationApi productPricingLocalizationApi) {
        this.productPricingLocalizationApi = productPricingLocalizationApi;
    }

    @Override
    protected Mono<UUID> doHandle(RegisterProductPricingLocalizationCommand cmd) {
        return productPricingLocalizationApi.createLocalization(cmd.getProductPricingId(), cmd.getProductPricingId(), cmd, UUID.randomUUID().toString())
                .mapNotNull(productPricingLocalizationDTO ->
                        Objects.requireNonNull(Objects.requireNonNull(productPricingLocalizationDTO)).getProductPricingLocalizationId());
    }
}
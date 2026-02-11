package com.firefly.domain.product.catalog.core.products.handlers;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.command.CommandHandler;
import com.firefly.common.product.sdk.api.ProductFeatureApi;
import com.firefly.domain.product.catalog.core.products.commands.RegisterProductFeatureCommand;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class RegisterProductFeatureHandler extends CommandHandler<RegisterProductFeatureCommand, UUID> {

    private final ProductFeatureApi productFeatureApi;

    public RegisterProductFeatureHandler(ProductFeatureApi productFeatureApi) {
        this.productFeatureApi = productFeatureApi;
    }

    @Override
    protected Mono<UUID> doHandle(RegisterProductFeatureCommand cmd) {
        return productFeatureApi.createFeature(cmd.getProductId(), cmd, UUID.randomUUID().toString())
                .mapNotNull(productFeatureDTO ->
                        Objects.requireNonNull(Objects.requireNonNull(productFeatureDTO)).getProductFeatureId());
    }
}
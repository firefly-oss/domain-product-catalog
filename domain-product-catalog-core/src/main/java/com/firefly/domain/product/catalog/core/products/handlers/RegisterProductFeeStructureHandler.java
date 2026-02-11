package com.firefly.domain.product.catalog.core.products.handlers;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.command.CommandHandler;
import com.firefly.common.product.sdk.api.ProductFeeStructureApi;
import com.firefly.domain.product.catalog.core.products.commands.RegisterProductFeeStructureCommand;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class RegisterProductFeeStructureHandler extends CommandHandler<RegisterProductFeeStructureCommand, UUID> {

    private final ProductFeeStructureApi productFeeStructureApi;

    public RegisterProductFeeStructureHandler(ProductFeeStructureApi productFeeStructureApi) {
        this.productFeeStructureApi = productFeeStructureApi;
    }

    @Override
    protected Mono<UUID> doHandle(RegisterProductFeeStructureCommand cmd) {
        return productFeeStructureApi.createFeeStructure(cmd.getProductId(), cmd, UUID.randomUUID().toString())
                .mapNotNull(productFeeStructureDTO ->
                        Objects.requireNonNull(Objects.requireNonNull(productFeeStructureDTO)).getProductFeeStructureId());
    }
}
package com.firefly.domain.product.catalog.core.products.handlers;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.command.CommandHandler;
import com.firefly.common.product.sdk.api.ProductFeeComponentApi;
import com.firefly.domain.product.catalog.core.products.commands.RemoveFeeComponentCommand;
import reactor.core.publisher.Mono;

import java.util.UUID;

@CommandHandlerComponent
public class RemoveFeeComponentHandler extends CommandHandler<RemoveFeeComponentCommand, Void> {

    private final ProductFeeComponentApi productFeeComponentApi;

    public RemoveFeeComponentHandler(ProductFeeComponentApi productFeeComponentApi) {
        this.productFeeComponentApi = productFeeComponentApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveFeeComponentCommand cmd) {
        return productFeeComponentApi.deleteFeeComponent(cmd.feeStructureId(), cmd.feeComponentId()).then();
    }
}
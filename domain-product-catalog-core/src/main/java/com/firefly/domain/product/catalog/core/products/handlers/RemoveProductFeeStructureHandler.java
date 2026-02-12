package com.firefly.domain.product.catalog.core.products.handlers;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.command.CommandHandler;
import com.firefly.core.product.sdk.api.ProductFeeStructureApi;
import com.firefly.domain.product.catalog.core.products.commands.RemoveProductFeeStructureCommand;
import reactor.core.publisher.Mono;

@CommandHandlerComponent
public class RemoveProductFeeStructureHandler extends CommandHandler<RemoveProductFeeStructureCommand, Void> {

    private final ProductFeeStructureApi productFeeStructureApi;

    public RemoveProductFeeStructureHandler(ProductFeeStructureApi productFeeStructureApi) {
        this.productFeeStructureApi = productFeeStructureApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveProductFeeStructureCommand cmd) {
        return productFeeStructureApi.deleteProductFeeStructure(cmd.productId(), cmd.productFeeStructureId()).then();
    }
}
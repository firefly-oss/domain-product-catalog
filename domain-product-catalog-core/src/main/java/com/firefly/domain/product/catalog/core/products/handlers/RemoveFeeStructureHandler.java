package com.firefly.domain.product.catalog.core.products.handlers;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.command.CommandHandler;
import com.firefly.common.product.sdk.api.FeeStructureApi;
import com.firefly.domain.product.catalog.core.products.commands.RemoveFeeStructureCommand;
import reactor.core.publisher.Mono;

@CommandHandlerComponent
public class RemoveFeeStructureHandler extends CommandHandler<RemoveFeeStructureCommand, Void> {

    private final FeeStructureApi feeStructureApi;

    public RemoveFeeStructureHandler(FeeStructureApi feeStructureApi) {
        this.feeStructureApi = feeStructureApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveFeeStructureCommand cmd) {
        return feeStructureApi.deleteFeeStructure(cmd.feeStructureId()).then();
    }
}
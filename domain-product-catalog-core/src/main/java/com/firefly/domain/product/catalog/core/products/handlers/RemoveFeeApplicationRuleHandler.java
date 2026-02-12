package com.firefly.domain.product.catalog.core.products.handlers;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.command.CommandHandler;
import com.firefly.core.product.sdk.api.FeeApplicationRuleApi;
import com.firefly.domain.product.catalog.core.products.commands.RemoveFeeApplicationRuleCommand;
import reactor.core.publisher.Mono;

@CommandHandlerComponent
public class RemoveFeeApplicationRuleHandler extends CommandHandler<RemoveFeeApplicationRuleCommand, Void> {

    private final FeeApplicationRuleApi feeApplicationRuleApi;

    public RemoveFeeApplicationRuleHandler(FeeApplicationRuleApi feeApplicationRuleApi) {
        this.feeApplicationRuleApi = feeApplicationRuleApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveFeeApplicationRuleCommand cmd) {
        return feeApplicationRuleApi.deleteRule(cmd.feeApplicationRuleId(), cmd.componentId(), cmd.feeApplicationRuleId()).then();
    }
}
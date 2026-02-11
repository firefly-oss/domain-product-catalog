package com.firefly.domain.product.catalog.core.products.handlers;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.command.CommandHandler;
import com.firefly.common.product.sdk.api.FeeApplicationRuleApi;
import com.firefly.domain.product.catalog.core.products.commands.RegisterFeeApplicationRuleCommand;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class RegisterFeeApplicationRuleHandler extends CommandHandler<RegisterFeeApplicationRuleCommand, UUID> {

    private final FeeApplicationRuleApi feeApplicationRuleApi;

    public RegisterFeeApplicationRuleHandler(FeeApplicationRuleApi feeApplicationRuleApi) {
        this.feeApplicationRuleApi = feeApplicationRuleApi;
    }

    @Override
    protected Mono<UUID> doHandle(RegisterFeeApplicationRuleCommand cmd) {
        return feeApplicationRuleApi.createRule(UUID.randomUUID(), cmd.getFeeComponentId(), cmd, UUID.randomUUID().toString())
                .mapNotNull(feeApplicationRuleDTO ->
                        Objects.requireNonNull(Objects.requireNonNull(feeApplicationRuleDTO)).getFeeApplicationRuleId());
    }
}
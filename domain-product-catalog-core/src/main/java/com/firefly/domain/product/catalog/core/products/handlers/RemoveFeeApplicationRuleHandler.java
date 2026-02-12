package com.firefly.domain.product.catalog.core.products.handlers;

import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import com.firefly.core.product.sdk.api.ProductConfigurationApi;
import com.firefly.domain.product.catalog.core.products.commands.RemoveFeeApplicationRuleCommand;
import reactor.core.publisher.Mono;

@CommandHandlerComponent
public class RemoveFeeApplicationRuleHandler extends CommandHandler<RemoveFeeApplicationRuleCommand, Void> {

    private final ProductConfigurationApi productConfigurationApi;

    public RemoveFeeApplicationRuleHandler(ProductConfigurationApi productConfigurationApi) {
        this.productConfigurationApi = productConfigurationApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveFeeApplicationRuleCommand cmd) {
        return productConfigurationApi.deleteConfiguration(cmd.productId(), cmd.feeApplicationRuleId(), null).then();
    }
}

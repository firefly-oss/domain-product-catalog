package com.firefly.domain.product.catalog.core.products.handlers;

import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import com.firefly.core.product.sdk.api.ProductConfigurationApi;
import com.firefly.domain.product.catalog.core.products.commands.RemoveProductPricingLocalizationCommand;
import reactor.core.publisher.Mono;

@CommandHandlerComponent
public class RemoveProductPricingLocalizationHandler extends CommandHandler<RemoveProductPricingLocalizationCommand, Void> {

    private final ProductConfigurationApi productConfigurationApi;

    public RemoveProductPricingLocalizationHandler(ProductConfigurationApi productConfigurationApi) {
        this.productConfigurationApi = productConfigurationApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveProductPricingLocalizationCommand cmd) {
        return productConfigurationApi.deleteConfiguration(cmd.productId(), cmd.productPricingLocalizationId(), null).then();
    }
}

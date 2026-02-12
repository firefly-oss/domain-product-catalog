package com.firefly.domain.product.catalog.core.products.handlers;

import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import com.firefly.core.product.sdk.api.ProductLocalizationApi;
import com.firefly.domain.product.catalog.core.products.commands.RemoveProductLocalizationCommand;
import reactor.core.publisher.Mono;

@CommandHandlerComponent
public class RemoveProductLocalizationHandler extends CommandHandler<RemoveProductLocalizationCommand, Void> {

    private final ProductLocalizationApi productLocalizationApi;

    public RemoveProductLocalizationHandler(ProductLocalizationApi productLocalizationApi) {
        this.productLocalizationApi = productLocalizationApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveProductLocalizationCommand cmd) {
        return productLocalizationApi.deleteLocalization(cmd.productId(), cmd.productLocalizationId(), null).then();
    }
}
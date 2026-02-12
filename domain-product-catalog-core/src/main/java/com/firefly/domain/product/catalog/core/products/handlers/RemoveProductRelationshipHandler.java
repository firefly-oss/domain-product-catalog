package com.firefly.domain.product.catalog.core.products.handlers;

import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import com.firefly.core.product.sdk.api.ProductRelationshipApi;
import com.firefly.domain.product.catalog.core.products.commands.RemoveProductRelationshipCommand;
import reactor.core.publisher.Mono;

@CommandHandlerComponent
public class RemoveProductRelationshipHandler extends CommandHandler<RemoveProductRelationshipCommand, Void> {

    private final ProductRelationshipApi productRelationshipApi;

    public RemoveProductRelationshipHandler(ProductRelationshipApi productRelationshipApi) {
        this.productRelationshipApi = productRelationshipApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveProductRelationshipCommand cmd) {
        return productRelationshipApi.deleteRelationship(cmd.productId(), cmd.productRelationshipId(), null).then();
    }
}
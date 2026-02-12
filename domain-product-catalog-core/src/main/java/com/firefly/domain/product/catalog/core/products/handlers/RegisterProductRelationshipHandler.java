package com.firefly.domain.product.catalog.core.products.handlers;

import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import com.firefly.core.product.sdk.api.ProductRelationshipApi;
import com.firefly.domain.product.catalog.core.products.commands.RegisterProductRelationshipCommand;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class RegisterProductRelationshipHandler extends CommandHandler<RegisterProductRelationshipCommand, UUID> {

    private final ProductRelationshipApi productRelationshipApi;

    public RegisterProductRelationshipHandler(ProductRelationshipApi productRelationshipApi) {
        this.productRelationshipApi = productRelationshipApi;
    }

    @Override
    protected Mono<UUID> doHandle(RegisterProductRelationshipCommand cmd) {
        return productRelationshipApi.createRelationship(cmd.getProductId(), cmd, UUID.randomUUID().toString())
                .mapNotNull(relationshipDTO ->
                        Objects.requireNonNull(Objects.requireNonNull(relationshipDTO)).getProductRelationshipId());
    }
}
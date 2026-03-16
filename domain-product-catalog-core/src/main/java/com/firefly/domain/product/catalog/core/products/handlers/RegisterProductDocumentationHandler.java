package com.firefly.domain.product.catalog.core.products.handlers;

import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import com.firefly.core.product.sdk.api.ProductDocumentationApi;
import com.firefly.domain.product.catalog.core.products.commands.RegisterProductDocumentationCommand;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class RegisterProductDocumentationHandler extends CommandHandler<RegisterProductDocumentationCommand, UUID> {

    private final ProductDocumentationApi productDocumentationApi;

    public RegisterProductDocumentationHandler(ProductDocumentationApi productDocumentationApi) {
        this.productDocumentationApi = productDocumentationApi;
    }

    @Override
    protected Mono<UUID> doHandle(RegisterProductDocumentationCommand cmd) {
        return productDocumentationApi.createDocumentation(cmd.getProductId(), cmd, UUID.randomUUID().toString())
                .mapNotNull(productDocumentationDTO ->
                        Objects.requireNonNull(Objects.requireNonNull(productDocumentationDTO)).getProductDocumentationId());
    }
}
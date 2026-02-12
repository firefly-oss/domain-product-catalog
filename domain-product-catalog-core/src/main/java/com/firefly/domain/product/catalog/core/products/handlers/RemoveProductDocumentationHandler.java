package com.firefly.domain.product.catalog.core.products.handlers;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.command.CommandHandler;
import com.firefly.core.product.sdk.api.ProductDocumentationApi;
import com.firefly.domain.product.catalog.core.products.commands.RemoveProductDocumentationCommand;
import reactor.core.publisher.Mono;

@CommandHandlerComponent
public class RemoveProductDocumentationHandler extends CommandHandler<RemoveProductDocumentationCommand, Void> {

    private final ProductDocumentationApi productDocumentationApi;

    public RemoveProductDocumentationHandler(ProductDocumentationApi productDocumentationApi) {
        this.productDocumentationApi = productDocumentationApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveProductDocumentationCommand cmd) {
        return productDocumentationApi.deleteDocumentation(cmd.productId(), cmd.productDocumentationId()).then();
    }
}
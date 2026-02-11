package com.firefly.domain.product.catalog.core.products.handlers;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.command.CommandHandler;
import com.firefly.common.product.sdk.api.ProductDocumentationRequirementsApi;
import com.firefly.domain.product.catalog.core.products.commands.RemoveProductDocumentationRequirementCommand;
import reactor.core.publisher.Mono;

@CommandHandlerComponent
public class RemoveProductDocumentationRequirementHandler extends CommandHandler<RemoveProductDocumentationRequirementCommand, Void> {

    private final ProductDocumentationRequirementsApi productDocumentationRequirementsApi;

    public RemoveProductDocumentationRequirementHandler(ProductDocumentationRequirementsApi productDocumentationRequirementsApi) {
        this.productDocumentationRequirementsApi = productDocumentationRequirementsApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveProductDocumentationRequirementCommand cmd) {
        return productDocumentationRequirementsApi.deleteDocumentationRequirement(cmd.productId(), cmd.productDocumentationRequirementId()).then();
    }
}
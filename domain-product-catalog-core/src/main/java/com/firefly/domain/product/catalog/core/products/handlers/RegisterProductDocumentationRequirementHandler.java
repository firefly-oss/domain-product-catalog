package com.firefly.domain.product.catalog.core.products.handlers;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.command.CommandHandler;
import com.firefly.common.product.sdk.api.ProductDocumentationRequirementsApi;
import com.firefly.domain.product.catalog.core.products.commands.RegisterProductDocumentationRequirementCommand;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class RegisterProductDocumentationRequirementHandler extends CommandHandler<RegisterProductDocumentationRequirementCommand, UUID> {

    private final ProductDocumentationRequirementsApi productDocumentationRequirementsApi;

    public RegisterProductDocumentationRequirementHandler(ProductDocumentationRequirementsApi productDocumentationRequirementsApi) {
        this.productDocumentationRequirementsApi = productDocumentationRequirementsApi;
    }

    @Override
    protected Mono<UUID> doHandle(RegisterProductDocumentationRequirementCommand cmd) {
        return productDocumentationRequirementsApi.createDocumentationRequirement(cmd.getProductId(), cmd, UUID.randomUUID().toString())
                .mapNotNull(productDocumentationDTO ->
                        Objects.requireNonNull(Objects.requireNonNull(productDocumentationDTO)).getProductDocRequirementId());
    }
}
package io.fireflysoftware.domain.product.catalog.core.products.handlers;

import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import io.fireflysoftware.core.product.sdk.api.ProductDocumentationRequirementsApi;
import io.fireflysoftware.domain.product.catalog.core.products.commands.RegisterProductDocumentationRequirementCommand;
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
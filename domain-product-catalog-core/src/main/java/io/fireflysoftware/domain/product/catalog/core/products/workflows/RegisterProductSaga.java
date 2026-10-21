package io.fireflysoftware.domain.product.catalog.core.products.workflows;

import io.fireflysoftware.core.product.sdk.model.ProductDTO;
import io.fireflysoftware.domain.product.catalog.core.products.commands.RegisterProductCategoryCommand;
import io.fireflysoftware.domain.product.catalog.core.products.commands.RegisterProductConfigurationCommand;
import io.fireflysoftware.domain.product.catalog.core.products.commands.RegisterProductDocumentationCommand;
import io.fireflysoftware.domain.product.catalog.core.products.commands.RegisterProductDocumentationRequirementCommand;
import io.fireflysoftware.domain.product.catalog.core.products.commands.RegisterProductInfoCommand;
import io.fireflysoftware.domain.product.catalog.core.products.commands.RegisterProductLocalizationCommand;
import io.fireflysoftware.domain.product.catalog.core.products.commands.RegisterProductRelationshipCommand;
import io.fireflysoftware.domain.product.catalog.core.products.commands.RegisterProductVersionCommand;
import io.fireflysoftware.domain.product.catalog.core.products.commands.RemoveProductCategoryCommand;
import io.fireflysoftware.domain.product.catalog.core.products.commands.RemoveProductCommand;
import io.fireflysoftware.domain.product.catalog.core.products.commands.RemoveProductConfigurationCommand;
import io.fireflysoftware.domain.product.catalog.core.products.commands.RemoveProductDocumentationCommand;
import io.fireflysoftware.domain.product.catalog.core.products.commands.RemoveProductDocumentationRequirementCommand;
import io.fireflysoftware.domain.product.catalog.core.products.commands.RemoveProductLocalizationCommand;
import io.fireflysoftware.domain.product.catalog.core.products.commands.RemoveProductRelationshipCommand;
import io.fireflysoftware.domain.product.catalog.core.products.commands.RemoveProductVersionCommand;
import org.fireflyframework.cqrs.command.CommandBus;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.fireflyframework.orchestration.saga.annotation.Saga;
import org.fireflyframework.orchestration.saga.annotation.SagaStep;
import org.fireflyframework.orchestration.saga.annotation.StepEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static io.fireflysoftware.domain.product.catalog.core.utils.constants.GlobalConstants.CTX_PRODUCT_CATEGORY_ID;
import static io.fireflysoftware.domain.product.catalog.core.utils.constants.GlobalConstants.CTX_PRODUCT_ID;
import static io.fireflysoftware.domain.product.catalog.core.utils.constants.RegisterProductConstants.COMPENSATE_REMOVE_PRODUCT;
import static io.fireflysoftware.domain.product.catalog.core.utils.constants.RegisterProductConstants.COMPENSATE_REMOVE_PRODUCT_CATEGORY;
import static io.fireflysoftware.domain.product.catalog.core.utils.constants.RegisterProductConstants.COMPENSATE_REMOVE_PRODUCT_DOCUMENTATION;
import static io.fireflysoftware.domain.product.catalog.core.utils.constants.RegisterProductConstants.COMPENSATE_REMOVE_PRODUCT_DOCUMENTATION_REQUIREMENT;
import static io.fireflysoftware.domain.product.catalog.core.utils.constants.RegisterProductConstants.COMPENSATE_REMOVE_PRODUCT_LOCALIZATION;
import static io.fireflysoftware.domain.product.catalog.core.utils.constants.RegisterProductConstants.COMPENSATE_REMOVE_PRODUCT_RELATIONSHIP;
import static io.fireflysoftware.domain.product.catalog.core.utils.constants.RegisterProductConstants.COMPENSATE_REMOVE_VERSION;
import static io.fireflysoftware.domain.product.catalog.core.utils.constants.RegisterProductConstants.EVENT_PRODUCT_CATEGORY_REGISTERED;
import static io.fireflysoftware.domain.product.catalog.core.utils.constants.RegisterProductConstants.EVENT_PRODUCT_DOCUMENTATION_REGISTERED;
import static io.fireflysoftware.domain.product.catalog.core.utils.constants.RegisterProductConstants.EVENT_PRODUCT_DOCUMENTATION_REQUIREMENT_REGISTERED;
import static io.fireflysoftware.domain.product.catalog.core.utils.constants.RegisterProductConstants.EVENT_PRODUCT_LOCALIZATION_REGISTERED;
import static io.fireflysoftware.domain.product.catalog.core.utils.constants.RegisterProductConstants.EVENT_PRODUCT_REGISTERED;
import static io.fireflysoftware.domain.product.catalog.core.utils.constants.RegisterProductConstants.EVENT_PRODUCT_RELATIONSHIP_REGISTERED;
import static io.fireflysoftware.domain.product.catalog.core.utils.constants.RegisterProductConstants.EVENT_VERSION_REGISTERED;
import static io.fireflysoftware.domain.product.catalog.core.utils.constants.RegisterProductConstants.SAGA_REGISTER_PRODUCT;
import static io.fireflysoftware.domain.product.catalog.core.utils.constants.RegisterProductConstants.STEP_REGISTER_PRODUCT;
import static io.fireflysoftware.domain.product.catalog.core.utils.constants.RegisterProductConstants.STEP_REGISTER_PRODUCT_CATEGORY;
import static io.fireflysoftware.domain.product.catalog.core.utils.constants.RegisterProductConstants.STEP_REGISTER_PRODUCT_DOCUMENTATION;
import static io.fireflysoftware.domain.product.catalog.core.utils.constants.RegisterProductConstants.STEP_REGISTER_PRODUCT_DOCUMENTATION_REQUIREMENT;
import static io.fireflysoftware.domain.product.catalog.core.utils.constants.RegisterProductConstants.STEP_REGISTER_PRODUCT_LOCALIZATION;
import static io.fireflysoftware.domain.product.catalog.core.utils.constants.RegisterProductConstants.STEP_REGISTER_PRODUCT_RELATIONSHIP;
import static io.fireflysoftware.domain.product.catalog.core.utils.constants.RegisterProductConstants.STEP_REGISTER_VERSION;

/**
 * Registers a product with the aggregates the product core still stores:
 * category, product, versions, relationships, localizations, documents, and
 * product configuration. Fees, features, limits, lifecycle rows, and bundles
 * are not posted. A product without loan pricing configuration still succeeds.
 */
@Saga(name = SAGA_REGISTER_PRODUCT)
@Service
public class RegisterProductSaga {

    private static final UUID SKIPPED = new UUID(0L, 0L);

    private final CommandBus commandBus;

    @Autowired
    public RegisterProductSaga(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @SagaStep(id = STEP_REGISTER_PRODUCT_CATEGORY, compensate = COMPENSATE_REMOVE_PRODUCT_CATEGORY)
    @StepEvent(type = EVENT_PRODUCT_CATEGORY_REGISTERED)
    public Mono<UUID> registerProductCategory(RegisterProductCategoryCommand cmd, ExecutionContext ctx) {
        if (cmd == null) {
            return Mono.just(SKIPPED);
        }
        return commandBus.send(cmd)
                .doOnNext(productCategoryId -> ctx.putVariable(CTX_PRODUCT_CATEGORY_ID, productCategoryId));
    }

    public Mono<Void> removeProductCategory(UUID productCategoryId) {
        if (productCategoryId == null || SKIPPED.equals(productCategoryId)) {
            return Mono.empty();
        }
        return commandBus.send(new RemoveProductCategoryCommand(productCategoryId));
    }

    @SagaStep(id = STEP_REGISTER_PRODUCT, compensate = COMPENSATE_REMOVE_PRODUCT, dependsOn = STEP_REGISTER_PRODUCT_CATEGORY)
    @StepEvent(type = EVENT_PRODUCT_REGISTERED)
    public Mono<UUID> registerProduct(RegisterProductInfoCommand cmd, ExecutionContext ctx) {
        UUID categoryId = ctx.getVariableAs(CTX_PRODUCT_CATEGORY_ID, UUID.class);
        if (categoryId != null && !SKIPPED.equals(categoryId) && cmd.getProductCategoryId() == null) {
            cmd.setProductCategoryId(categoryId);
        }
        if (cmd.getProductStatus() == null) {
            cmd.setProductStatus(ProductDTO.ProductStatusEnum.DRAFT);
        }
        return commandBus.send(cmd)
                .doOnNext(productId -> ctx.putVariable(CTX_PRODUCT_ID, productId));
    }

    public Mono<Void> removeProduct(UUID productId) {
        if (productId == null) {
            return Mono.empty();
        }
        return commandBus.send(new RemoveProductCommand(productId));
    }

    @SagaStep(id = "registerProductConfiguration", compensate = "removeProductConfiguration", dependsOn = STEP_REGISTER_PRODUCT)
    @StepEvent(type = "productConfiguration.registered")
    public Mono<UUID> registerProductConfiguration(RegisterProductConfigurationCommand cmd, ExecutionContext ctx) {
        if (cmd == null) {
            return Mono.just(SKIPPED);
        }
        return commandBus.send(cmd.withProductId(ctx.getVariableAs(CTX_PRODUCT_ID, UUID.class)));
    }

    public Mono<Void> removeProductConfiguration(UUID configurationId, ExecutionContext ctx) {
        if (configurationId == null || SKIPPED.equals(configurationId)) {
            return Mono.empty();
        }
        return commandBus.send(new RemoveProductConfigurationCommand(
                ctx.getVariableAs(CTX_PRODUCT_ID, UUID.class), configurationId));
    }

    @SagaStep(id = STEP_REGISTER_PRODUCT_RELATIONSHIP, compensate = COMPENSATE_REMOVE_PRODUCT_RELATIONSHIP, dependsOn = STEP_REGISTER_PRODUCT)
    @StepEvent(type = EVENT_PRODUCT_RELATIONSHIP_REGISTERED)
    public Mono<UUID> registerProductRelationship(RegisterProductRelationshipCommand cmd, ExecutionContext ctx) {
        if (cmd == null) {
            return Mono.just(SKIPPED);
        }
        return commandBus.send(cmd.withProductId(ctx.getVariableAs(CTX_PRODUCT_ID, UUID.class)));
    }

    public Mono<Void> removeProductRelationship(UUID productRelationshipId, ExecutionContext ctx) {
        if (productRelationshipId == null || SKIPPED.equals(productRelationshipId)) {
            return Mono.empty();
        }
        return commandBus.send(new RemoveProductRelationshipCommand(ctx.getVariableAs(CTX_PRODUCT_ID, UUID.class), productRelationshipId));
    }

    @SagaStep(id = STEP_REGISTER_PRODUCT_DOCUMENTATION, compensate = COMPENSATE_REMOVE_PRODUCT_DOCUMENTATION, dependsOn = STEP_REGISTER_PRODUCT)
    @StepEvent(type = EVENT_PRODUCT_DOCUMENTATION_REGISTERED)
    public Mono<UUID> registerProductDocumentation(RegisterProductDocumentationCommand cmd, ExecutionContext ctx) {
        if (cmd == null) {
            return Mono.just(SKIPPED);
        }
        return commandBus.send(cmd.withProductId(ctx.getVariableAs(CTX_PRODUCT_ID, UUID.class)));
    }

    public Mono<Void> removeProductDocumentation(UUID productDocumentationId, ExecutionContext ctx) {
        if (productDocumentationId == null || SKIPPED.equals(productDocumentationId)) {
            return Mono.empty();
        }
        return commandBus.send(new RemoveProductDocumentationCommand(ctx.getVariableAs(CTX_PRODUCT_ID, UUID.class), productDocumentationId));
    }

    @SagaStep(id = STEP_REGISTER_PRODUCT_DOCUMENTATION_REQUIREMENT, compensate = COMPENSATE_REMOVE_PRODUCT_DOCUMENTATION_REQUIREMENT, dependsOn = STEP_REGISTER_PRODUCT)
    @StepEvent(type = EVENT_PRODUCT_DOCUMENTATION_REQUIREMENT_REGISTERED)
    public Mono<UUID> registerProductDocumentationRequirement(RegisterProductDocumentationRequirementCommand cmd, ExecutionContext ctx) {
        if (cmd == null) {
            return Mono.just(SKIPPED);
        }
        return commandBus.send(cmd.withProductId(ctx.getVariableAs(CTX_PRODUCT_ID, UUID.class)));
    }

    public Mono<Void> removeProductDocumentationRequirement(UUID requirementId, ExecutionContext ctx) {
        if (requirementId == null || SKIPPED.equals(requirementId)) {
            return Mono.empty();
        }
        return commandBus.send(new RemoveProductDocumentationRequirementCommand(ctx.getVariableAs(CTX_PRODUCT_ID, UUID.class), requirementId));
    }

    @SagaStep(id = STEP_REGISTER_PRODUCT_LOCALIZATION, compensate = COMPENSATE_REMOVE_PRODUCT_LOCALIZATION, dependsOn = STEP_REGISTER_PRODUCT)
    @StepEvent(type = EVENT_PRODUCT_LOCALIZATION_REGISTERED)
    public Mono<UUID> registerProductLocalization(RegisterProductLocalizationCommand cmd, ExecutionContext ctx) {
        if (cmd == null) {
            return Mono.just(SKIPPED);
        }
        return commandBus.send(cmd.withProductId(ctx.getVariableAs(CTX_PRODUCT_ID, UUID.class)));
    }

    public Mono<Void> removeProductLocalization(UUID localizationId, ExecutionContext ctx) {
        if (localizationId == null || SKIPPED.equals(localizationId)) {
            return Mono.empty();
        }
        return commandBus.send(new RemoveProductLocalizationCommand(ctx.getVariableAs(CTX_PRODUCT_ID, UUID.class), localizationId));
    }

    @SagaStep(id = STEP_REGISTER_VERSION, compensate = COMPENSATE_REMOVE_VERSION, dependsOn = STEP_REGISTER_PRODUCT)
    @StepEvent(type = EVENT_VERSION_REGISTERED)
    public Mono<UUID> registerVersion(RegisterProductVersionCommand cmd, ExecutionContext ctx) {
        if (cmd == null) {
            return Mono.just(SKIPPED);
        }
        return commandBus.send(cmd.withProductId(ctx.getVariableAs(CTX_PRODUCT_ID, UUID.class)));
    }

    public Mono<Void> removeVersion(UUID productVersionId, ExecutionContext ctx) {
        if (productVersionId == null || SKIPPED.equals(productVersionId)) {
            return Mono.empty();
        }
        return commandBus.send(new RemoveProductVersionCommand(ctx.getVariableAs(CTX_PRODUCT_ID, UUID.class), productVersionId));
    }
}

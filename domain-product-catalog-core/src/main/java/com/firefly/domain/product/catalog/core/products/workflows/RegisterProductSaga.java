package com.firefly.domain.product.catalog.core.products.workflows;

import org.fireflyframework.cqrs.command.CommandBus;
import com.firefly.domain.product.catalog.core.products.commands.*;
import org.fireflyframework.orchestration.saga.annotation.Saga;
import org.fireflyframework.orchestration.saga.annotation.SagaStep;
import org.fireflyframework.orchestration.saga.annotation.StepEvent;
import org.fireflyframework.orchestration.core.context.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static com.firefly.domain.product.catalog.core.utils.constants.GlobalConstants.*;
import static com.firefly.domain.product.catalog.core.utils.constants.RegisterProductConstants.*;


@Saga(name = SAGA_REGISTER_PRODUCT)
@Service
public class RegisterProductSaga {

    private final CommandBus commandBus;

    @Autowired
    public RegisterProductSaga(CommandBus commandBus) {
        this.commandBus = commandBus;
    }

    @SagaStep(id = STEP_REGISTER_PRODUCT_CATEGORY, compensate = COMPENSATE_REMOVE_PRODUCT_CATEGORY)
    @StepEvent(type = EVENT_PRODUCT_CATEGORY_REGISTERED)
    public Mono<UUID> registerProductCategory(RegisterProductCategoryCommand cmd, ExecutionContext ctx) {
        return commandBus.send(cmd)
                .doOnNext(productCategoryId -> ctx.putVariable(CTX_PRODUCT_CATEGORY_ID, productCategoryId));
    }

    public Mono<Void> removeProductCategory(UUID productCategoryId) {
        return commandBus.send(new RemoveProductCategoryCommand(productCategoryId));
    }

    @SagaStep(id = STEP_REGISTER_PRODUCT_SUBTYPE, compensate = COMPENSATE_REMOVE_PRODUCT_SUBTYPE, dependsOn = STEP_REGISTER_PRODUCT_CATEGORY)
    @StepEvent(type = EVENT_PRODUCT_SUBTYPE_REGISTERED)
    public Mono<UUID> registerProductSubtype(RegisterProductSubtypeCommand cmd, ExecutionContext ctx) {
        return commandBus.send(cmd.withProductCategoryId(ctx.getVariableAs(CTX_PRODUCT_CATEGORY_ID, UUID.class)))
                .doOnNext(productSubtypeId -> ctx.putVariable(CTX_PRODUCT_SUBTYPE_ID, productSubtypeId));
    }

    public Mono<Void> removeProductSubtype(UUID productSubtypeId, ExecutionContext ctx) {
        return commandBus.send(new RemoveProductSubtypeCommand(ctx.getVariableAs(CTX_PRODUCT_ID, UUID.class), ctx.getVariableAs(CTX_PRODUCT_CATEGORY_ID, UUID.class), productSubtypeId));
    }

    @SagaStep(id = STEP_REGISTER_FEE_STRUCTURE, compensate = COMPENSATE_REMOVE_FEE_STRUCTURE)
    @StepEvent(type = EVENT_FEE_STRUCTURE_REGISTERED)
    public Mono<UUID> registerFeeStructure(RegisterFeeStructureCommand cmd, ExecutionContext ctx) {
        return commandBus.send(cmd)
                .doOnNext(freeStructureId -> ctx.putVariable(CTX_FEE_STRUCTURE_ID, freeStructureId));
    }

    public Mono<Void> removeFeeStructure(UUID feeStructureId, ExecutionContext ctx) {
        return commandBus.send(new RemoveFeeStructureCommand(ctx.getVariableAs(CTX_PRODUCT_ID, UUID.class), feeStructureId));
    }

    @SagaStep(id = STEP_REGISTER_PRODUCT_BUNDLE, compensate = COMPENSATE_REMOVE_PRODUCT_BUNDLE)
    @StepEvent(type = EVENT_PRODUCT_BUNDLE_REGISTERED)
    public Mono<UUID> registerProductBundle(RegisterProductBundleCommand cmd, ExecutionContext ctx) {
        return commandBus.send(cmd)
                .doOnNext(productBundleId -> ctx.putVariable(CTX_PRODUCT_BUNDLE_ID, productBundleId));
    }

    public Mono<Void> removeProductBundle(UUID productBundleId, ExecutionContext ctx) {
        return commandBus.send(new RemoveProductBundleCommand(ctx.getVariableAs(CTX_PRODUCT_ID, UUID.class), productBundleId));
    }

    @SagaStep(id = STEP_REGISTER_FEE_COMPONENT, compensate = COMPENSATE_REMOVE_FEE_COMPONENT, dependsOn = STEP_REGISTER_FEE_STRUCTURE)
    @StepEvent(type = EVENT_FEE_COMPONENT_REGISTERED)
    public Mono<UUID> registerFeeComponent(RegisterFeeComponentCommand cmd, ExecutionContext ctx) {
        return commandBus.send(cmd.withFeeStructureId(ctx.getVariableAs(CTX_FEE_STRUCTURE_ID, UUID.class)))
                .doOnNext(feeComponentId -> ctx.putVariable(CTX_FEE_COMPONENT_ID, feeComponentId));
    }

    public Mono<Void> removeFeeComponent(UUID feeComponentId, ExecutionContext ctx) {
        return commandBus.send(new RemoveFeeComponentCommand(ctx.getVariableAs(CTX_PRODUCT_ID, UUID.class), feeComponentId, ctx.getVariableAs(CTX_FEE_STRUCTURE_ID, UUID.class)));
    }

    @SagaStep(id = STEP_REGISTER_FEE_APPLICATION_RULE, compensate = COMPENSATE_REMOVE_FEE_APPLICATION_RULE, dependsOn = {STEP_REGISTER_FEE_COMPONENT, STEP_REGISTER_FEE_STRUCTURE})
    @StepEvent(type = EVENT_FEE_APPLICATION_RULE_REGISTERED)
    public Mono<UUID> registerFeeApplicationRule(RegisterFeeApplicationRuleCommand cmd, ExecutionContext ctx) {
        return commandBus.send(cmd
                .withFeeComponentId(ctx.getVariableAs(CTX_FEE_COMPONENT_ID, UUID.class)));
    }

    public Mono<Void> removeFeeApplicationRule(UUID feeApplicationRuleId, ExecutionContext ctx) {
        return commandBus.send(new RemoveFeeApplicationRuleCommand(
                ctx.getVariableAs(CTX_PRODUCT_ID, UUID.class),
                feeApplicationRuleId,
                ctx.getVariableAs(CTX_FEE_STRUCTURE_ID, UUID.class),
                ctx.getVariableAs(CTX_FEE_COMPONENT_ID, UUID.class)));
    }

    @SagaStep(id = STEP_REGISTER_PRODUCT, compensate = COMPENSATE_REMOVE_PRODUCT, dependsOn = {STEP_REGISTER_PRODUCT_SUBTYPE})
    @StepEvent(type = EVENT_PRODUCT_REGISTERED)
    public Mono<UUID> registerProduct(RegisterProductInfoCommand cmd, ExecutionContext ctx) {
        return commandBus.send(cmd.withProductSubtypeId(ctx.getVariableAs(CTX_PRODUCT_SUBTYPE_ID, UUID.class)))
                .doOnNext(productId -> ctx.putVariable(CTX_PRODUCT_ID, productId));
    }

    public Mono<Void> removeProduct(UUID productId) {
        return commandBus.send(new RemoveProductCommand(productId));
    }

    @SagaStep(id = STEP_REGISTER_PRODUCT_FEE_STRUCTURE, compensate = COMPENSATE_REMOVE_PRODUCT_FEE_STRUCTURE, dependsOn = {STEP_REGISTER_PRODUCT, STEP_REGISTER_FEE_STRUCTURE})
    @StepEvent(type = EVENT_PRODUCT_FEE_STRUCTURE_REGISTERED)
    public Mono<UUID> registerProductFeeStructure(RegisterProductFeeStructureCommand cmd, ExecutionContext ctx) {
        return commandBus.send(cmd
                        .withProductId(ctx.getVariableAs(CTX_PRODUCT_ID, UUID.class))
                        .withFeeStructureId(ctx.getVariableAs(CTX_FEE_STRUCTURE_ID, UUID.class)))
                .doOnNext(productFeeStructureId -> ctx.putVariable(CTX_PRODUCT_FEE_STRUCTURE_ID, productFeeStructureId));
    }

    public Mono<Void> removeProductFeeStructure(UUID productFeeStructureId, ExecutionContext ctx) {
        return commandBus.send(new RemoveProductFeeStructureCommand(ctx.getVariableAs(CTX_PRODUCT_ID, UUID.class), productFeeStructureId));
    }

    @SagaStep(id = STEP_REGISTER_PRODUCT_BUNDLE_ITEMS, compensate = COMPENSATE_REMOVE_PRODUCT_BUNDLE_ITEMS, dependsOn = {STEP_REGISTER_PRODUCT_BUNDLE, STEP_REGISTER_PRODUCT})
    @StepEvent(type = EVENT_PRODUCT_BUNDLE_ITEMS_REGISTERED)
    public Mono<UUID> registerProductBundleItems(RegisterProductBundleItemCommand cmd, ExecutionContext ctx) {
        return commandBus.send(cmd
                        .withProductBundleId(ctx.getVariableAs(CTX_PRODUCT_BUNDLE_ID, UUID.class))
                        .withProductId(ctx.getVariableAs(CTX_PRODUCT_ID, UUID.class)))
                .doOnNext(productBundleItemId -> ctx.putVariable(CTX_PRODUCT_BUNDLE_ITEM_ID, productBundleItemId));
    }

    public Mono<Void> removeProductBundleItems(UUID productBundleItemId, ExecutionContext ctx) {
        return commandBus.send(new RemoveProductBundleItemCommand(ctx.getVariableAs(CTX_PRODUCT_ID, UUID.class), ctx.getVariableAs(CTX_PRODUCT_BUNDLE_ID, UUID.class), productBundleItemId));
    }

    @SagaStep(id = STEP_REGISTER_PRODUCT_PRICING, compensate = COMPENSATE_REMOVE_PRODUCT_PRICING, dependsOn = {STEP_REGISTER_PRODUCT})
    @StepEvent(type = EVENT_PRODUCT_PRICING_REGISTERED)
    public Mono<UUID> registerProductPricing(RegisterProductPricingCommand cmd, ExecutionContext ctx) {
        return commandBus.send(cmd.withProductId(ctx.getVariableAs(CTX_PRODUCT_ID, UUID.class)))
                .doOnNext(productPricingId -> ctx.putVariable(CTX_PRODUCT_PRICING_ID, productPricingId));
    }

    public Mono<Void> removeProductPricing(UUID productPricingId, ExecutionContext ctx) {
        return commandBus.send(new RemoveProductPricingCommand(ctx.getVariableAs(CTX_PRODUCT_ID, UUID.class), productPricingId));
    }

    @SagaStep(id = STEP_REGISTER_PRODUCT_RELATIONSHIP, compensate = COMPENSATE_REMOVE_PRODUCT_RELATIONSHIP, dependsOn = {STEP_REGISTER_PRODUCT})
    @StepEvent(type = EVENT_PRODUCT_RELATIONSHIP_REGISTERED)
    public Mono<UUID> registerProductRelationship(RegisterProductRelationshipCommand cmd, ExecutionContext ctx) {
        return commandBus.send(cmd.withProductId(ctx.getVariableAs(CTX_PRODUCT_ID, UUID.class)))
                .doOnNext(productRelationshipId -> ctx.putVariable(CTX_PRODUCT_RELATIONSHIP_ID, productRelationshipId));
    }

    public Mono<Void> removeProductRelationship(UUID productRelationshipId, ExecutionContext ctx) {
        return commandBus.send(new RemoveProductRelationshipCommand(ctx.getVariableAs(CTX_PRODUCT_ID, UUID.class), productRelationshipId));
    }

    @SagaStep(id = STEP_REGISTER_PRODUCT_DOCUMENTATION, compensate = COMPENSATE_REMOVE_PRODUCT_DOCUMENTATION, dependsOn = {STEP_REGISTER_PRODUCT})
    @StepEvent(type = EVENT_PRODUCT_DOCUMENTATION_REGISTERED)
    public Mono<UUID> registerProductDocumentation(RegisterProductDocumentationCommand cmd, ExecutionContext ctx) {
        return commandBus.send(cmd.withProductId(ctx.getVariableAs(CTX_PRODUCT_ID, UUID.class)))
                .doOnNext(productDocumentationId -> ctx.putVariable(CTX_PRODUCT_DOCUMENTATION_ID, productDocumentationId));
    }

    public Mono<Void> removeProductDocumentation(UUID productDocumentationId, ExecutionContext ctx) {
        return commandBus.send(new RemoveProductDocumentationCommand(ctx.getVariableAs(CTX_PRODUCT_ID, UUID.class), productDocumentationId));
    }

    @SagaStep(id = STEP_REGISTER_PRODUCT_DOCUMENTATION_REQUIREMENT, compensate = COMPENSATE_REMOVE_PRODUCT_DOCUMENTATION_REQUIREMENT, dependsOn = {STEP_REGISTER_PRODUCT})
    @StepEvent(type = EVENT_PRODUCT_DOCUMENTATION_REQUIREMENT_REGISTERED)
    public Mono<UUID> registerProductDocumentationRequirement(RegisterProductDocumentationRequirementCommand cmd, ExecutionContext ctx) {
        return commandBus.send(cmd.withProductId(ctx.getVariableAs(CTX_PRODUCT_ID, UUID.class)))
                .doOnNext(productDocumentationRequirementId -> ctx.putVariable(CTX_PRODUCT_DOCUMENTATION_REQUIREMENT_ID, productDocumentationRequirementId));
    }

    public Mono<Void> removeProductDocumentationRequirement(UUID productDocumentationRequirementId, ExecutionContext ctx) {
        return commandBus.send(new RemoveProductDocumentationRequirementCommand(ctx.getVariableAs(CTX_PRODUCT_ID, UUID.class), productDocumentationRequirementId));
    }

    @SagaStep(id = STEP_REGISTER_PRODUCT_FEATURES, compensate = COMPENSATE_REMOVE_PRODUCT_FEATURES, dependsOn = {STEP_REGISTER_PRODUCT})
    @StepEvent(type = EVENT_PRODUCT_FEATURES_REGISTERED)
    public Mono<UUID> registerProductFeatures(RegisterProductFeatureCommand cmd, ExecutionContext ctx) {
        return commandBus.send(cmd.withProductId(ctx.getVariableAs(CTX_PRODUCT_ID, UUID.class)))
                .doOnNext(productFeaturesId -> ctx.putVariable(CTX_PRODUCT_FEATURES_ID, productFeaturesId));
    }

    public Mono<Void> removeProductFeatures(UUID productFeaturesId, ExecutionContext ctx) {
        return commandBus.send(new RemoveProductFeatureCommand(ctx.getVariableAs(CTX_PRODUCT_ID, UUID.class), productFeaturesId));
    }

    @SagaStep(id = STEP_REGISTER_PRODUCT_LIFECYCLE, compensate = COMPENSATE_REMOVE_PRODUCT_LIFECYCLE, dependsOn = {STEP_REGISTER_PRODUCT})
    @StepEvent(type = EVENT_PRODUCT_LIFECYCLE_REGISTERED)
    public Mono<UUID> registerProductLifecycle(RegisterProductLifecycleCommand cmd, ExecutionContext ctx) {
        return commandBus.send(cmd.withProductId(ctx.getVariableAs(CTX_PRODUCT_ID, UUID.class)))
                .doOnNext(productLifecycleId -> ctx.putVariable(CTX_PRODUCT_LIFECYCLE_ID, productLifecycleId));
    }

    public Mono<Void> removeProductLifecycle(UUID productLifecycleId, ExecutionContext ctx) {
        return commandBus.send(new RemoveProductLifecycleCommand(ctx.getVariableAs(CTX_PRODUCT_ID, UUID.class), productLifecycleId));
    }

    @SagaStep(id = STEP_REGISTER_PRODUCT_LIMITS, compensate = COMPENSATE_REMOVE_PRODUCT_LIMITS, dependsOn = {STEP_REGISTER_PRODUCT})
    @StepEvent(type = EVENT_PRODUCT_LIMITS_REGISTERED)
    public Mono<UUID> registerProductLimits(RegisterProductLimitCommand cmd, ExecutionContext ctx) {
        return commandBus.send(cmd.withProductId(ctx.getVariableAs(CTX_PRODUCT_ID, UUID.class)))
                .doOnNext(productLimitsId -> ctx.putVariable(CTX_PRODUCT_LIMITS_ID, productLimitsId));
    }

    public Mono<Void> removeProductLimits(UUID productLimitsId, ExecutionContext ctx) {
        return commandBus.send(new RemoveProductLimitCommand(ctx.getVariableAs(CTX_PRODUCT_ID, UUID.class), productLimitsId));
    }

    @SagaStep(id = STEP_REGISTER_PRODUCT_LOCALIZATION, compensate = COMPENSATE_REMOVE_PRODUCT_LOCALIZATION, dependsOn = {STEP_REGISTER_PRODUCT})
    @StepEvent(type = EVENT_PRODUCT_LOCALIZATION_REGISTERED)
    public Mono<UUID> registerProductLocalization(RegisterProductLocalizationCommand cmd, ExecutionContext ctx) {
        return commandBus.send(cmd.withProductId(ctx.getVariableAs(CTX_PRODUCT_ID, UUID.class)))
                .doOnNext(productLocalizationId -> ctx.putVariable(CTX_PRODUCT_LOCALIZATION_ID, productLocalizationId));
    }

    public Mono<Void> removeProductLocalization(UUID productLocalizationId, ExecutionContext ctx) {
        return commandBus.send(new RemoveProductLocalizationCommand(ctx.getVariableAs(CTX_PRODUCT_ID, UUID.class), productLocalizationId));
    }

    @SagaStep(id = STEP_REGISTER_VERSION, compensate = COMPENSATE_REMOVE_VERSION, dependsOn = {STEP_REGISTER_PRODUCT})
    @StepEvent(type = EVENT_VERSION_REGISTERED)
    public Mono<UUID> registerVersion(RegisterProductVersionCommand cmd, ExecutionContext ctx) {
        return commandBus.send(cmd.withProductId(ctx.getVariableAs(CTX_PRODUCT_ID, UUID.class)))
                .doOnNext(productVersionId -> ctx.putVariable(CTX_PRODUCT_VERSION_ID, productVersionId));
    }

    public Mono<Void> removeVersion(UUID productVersionId, ExecutionContext ctx) {
        return commandBus.send(new RemoveProductVersionCommand(ctx.getVariableAs(CTX_PRODUCT_ID, UUID.class), productVersionId));
    }

    @SagaStep(id = STEP_REGISTER_PRODUCT_PRICING_LOCALIZATION, compensate = COMPENSATE_REMOVE_PRODUCT_PRICING_LOCALIZATION, dependsOn = {STEP_REGISTER_PRODUCT, STEP_REGISTER_PRODUCT_PRICING})
    @StepEvent(type = EVENT_PRODUCT_PRICING_LOCALIZATION_REGISTERED)
    public Mono<UUID> registerProductPricingLocalization(RegisterProductPricingLocalizationCommand cmd, ExecutionContext ctx) {
        return commandBus.send(cmd
                        .withProductPricingId(ctx.getVariableAs(CTX_PRODUCT_PRICING_ID, UUID.class)))
                .doOnNext(productPricingLocalizationId -> ctx.putVariable(CTX_PRODUCT_PRICING_LOCALIZATION_ID, productPricingLocalizationId));
    }

    public Mono<Void> removeProductPricingLocalization(UUID productPricingLocalizationId, ExecutionContext ctx) {
        return commandBus.send(new RemoveProductPricingLocalizationCommand(
                ctx.getVariableAs(CTX_PRODUCT_ID, UUID.class),
                ctx.getVariableAs(CTX_PRODUCT_PRICING_ID, UUID.class),
                productPricingLocalizationId));
    }

}

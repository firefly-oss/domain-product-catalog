package io.fireflysoftware.domain.product.catalog.core.products.commands;

import org.fireflyframework.cqrs.command.Command;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * Registers a product. Pricing, limits, and fees are optional configuration rows.
 * A product that is not a loan does not need them.
 */
@Data
public class RegisterProductCommand implements Command<UUID> {
    private RegisterProductInfoCommand product;
    private RegisterProductCategoryCommand productCategory;
    private List<RegisterProductConfigurationCommand> productConfigurations;
    private List<RegisterProductDocumentationCommand> productDocumentation;
    private List<RegisterProductDocumentationRequirementCommand> productDocumentationRequirements;
    private List<RegisterProductLocalizationCommand> productLocalizations;
    private List<RegisterProductVersionCommand> productVersions;
    private List<RegisterProductRelationshipCommand> productRelationships;
}

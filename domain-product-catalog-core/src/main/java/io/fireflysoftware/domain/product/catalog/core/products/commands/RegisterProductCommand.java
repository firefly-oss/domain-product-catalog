/*
 * Copyright 2025 Firefly Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


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

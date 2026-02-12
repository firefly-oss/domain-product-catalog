/*
 * Copyright 2025 Firefly Software Solutions Inc
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

package com.firefly.domain.product.catalog.core.products.commands;

import org.fireflyframework.cqrs.command.Command;
import com.firefly.core.product.sdk.model.ProductDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;
import java.util.UUID;

@Data
public class RegisterProductCommand implements Command<UUID> {
    private RegisterProductInfoCommand product;
    private List<RegisterProductFeatureCommand> productFeatures;
    private RegisterProductPricingCommand productPricing;
    private List<RegisterProductDocumentationCommand> productDocumentation;
    private List<RegisterProductLifecycleCommand> productLifecycle;
    private List<RegisterProductLimitCommand> productLimits;
    private List<RegisterProductLocalizationCommand> productLocalizations;
    private List<RegisterProductVersionCommand> productVersions;
    private List<RegisterProductFeeStructureCommand> productFeeStructures;
    private List<RegisterProductRelationshipCommand> productRelationships;
    private List<RegisterProductDocumentationRequirementCommand> productDocumentationRequirements;
    private RegisterProductCategoryCommand productCategory;
    private RegisterProductSubtypeCommand productSubtype;
    private RegisterProductBundleCommand productBundle;
    private RegisterFeeApplicationRuleCommand feeApplicationRule;
    private RegisterFeeComponentCommand feeComponent;
    private RegisterFeeStructureCommand feeStructure;
    private List<RegisterProductBundleItemCommand> productBundleItems;
    private RegisterProductPricingLocalizationCommand productPricingLocalization;
}


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


package io.fireflysoftware.domain.product.catalog.infra;

import io.fireflysoftware.core.product.sdk.api.ProductApi;
import io.fireflysoftware.core.product.sdk.api.ProductCategoryApi;
import io.fireflysoftware.core.product.sdk.api.ProductConfigurationApi;
import io.fireflysoftware.core.product.sdk.api.ProductDocumentationApi;
import io.fireflysoftware.core.product.sdk.api.ProductDocumentationRequirementsApi;
import io.fireflysoftware.core.product.sdk.api.ProductLocalizationApi;
import io.fireflysoftware.core.product.sdk.api.ProductRelationshipApi;
import io.fireflysoftware.core.product.sdk.api.ProductVersionApi;
import io.fireflysoftware.core.product.sdk.invoker.ApiClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * Default implementation of the ClientFactory interface.
 * Creates client service instances using the appropriate API clients and dependencies.
 */
@Component
public class ClientFactory {

    private final ApiClient apiClient;

    @Autowired
    public ClientFactory(
            ProductMgmtProperties productMgmtProperties) {
        this.apiClient = new ApiClient();
        this.apiClient.setBasePath(productMgmtProperties.getBasePath());
    }

    @Bean
    public ProductCategoryApi partiesApi() {
        return new ProductCategoryApi(apiClient);
    }

    @Bean
    public ProductConfigurationApi productConfigurationApi() {
        return new ProductConfigurationApi(apiClient);
    }

    @Bean
    public ProductApi productApi() {
        return new ProductApi(apiClient);
    }

    @Bean
    public ProductRelationshipApi productRelationshipApi() {
        return new ProductRelationshipApi(apiClient);
    }

    @Bean
    public ProductDocumentationApi productDocumentationApi() {
        return new ProductDocumentationApi(apiClient);
    }

    @Bean
    public ProductDocumentationRequirementsApi productDocumentationRequirementsApi() {
        return new ProductDocumentationRequirementsApi(apiClient);
    }

    @Bean
    public ProductLocalizationApi productLocalizationApi() {
        return new ProductLocalizationApi(apiClient);
    }

    @Bean
    public ProductVersionApi productVersionApi() {
        return new ProductVersionApi(apiClient);
    }

}

package com.firefly.domain.product.catalog.infra;

import com.firefly.core.product.sdk.api.ProductApi;
import com.firefly.core.product.sdk.api.ProductCategoryApi;
import com.firefly.core.product.sdk.api.ProductConfigurationApi;
import com.firefly.core.product.sdk.api.ProductDocumentationApi;
import com.firefly.core.product.sdk.api.ProductDocumentationRequirementsApi;
import com.firefly.core.product.sdk.api.ProductLocalizationApi;
import com.firefly.core.product.sdk.api.ProductRelationshipApi;
import com.firefly.core.product.sdk.api.ProductVersionApi;
import com.firefly.core.product.sdk.invoker.ApiClient;
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

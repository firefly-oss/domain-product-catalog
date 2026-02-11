package com.firefly.domain.product.catalog.infra;

import com.firefly.common.product.sdk.api.FeeApplicationRuleApi;
import com.firefly.common.product.sdk.api.FeeStructureApi;
import com.firefly.common.product.sdk.api.ProductApi;
import com.firefly.common.product.sdk.api.ProductBundleApi;
import com.firefly.common.product.sdk.api.ProductBundleItemApi;
import com.firefly.common.product.sdk.api.ProductCategoryApi;
import com.firefly.common.product.sdk.api.ProductDocumentationApi;
import com.firefly.common.product.sdk.api.ProductDocumentationRequirementsApi;
import com.firefly.common.product.sdk.api.ProductFeatureApi;
import com.firefly.common.product.sdk.api.ProductFeeComponentApi;
import com.firefly.common.product.sdk.api.ProductFeeStructureApi;
import com.firefly.common.product.sdk.api.ProductLifecycleApi;
import com.firefly.common.product.sdk.api.ProductLimitApi;
import com.firefly.common.product.sdk.api.ProductLocalizationApi;
import com.firefly.common.product.sdk.api.ProductPricingApi;
import com.firefly.common.product.sdk.api.ProductPricingLocalizationApi;
import com.firefly.common.product.sdk.api.ProductRelationshipApi;
import com.firefly.common.product.sdk.api.ProductSubtypeApi;
import com.firefly.common.product.sdk.api.ProductVersionApi;
import com.firefly.common.product.sdk.invoker.ApiClient;
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
    public ProductFeeStructureApi productFeeStructureApi() {
        return new ProductFeeStructureApi(apiClient);
    }

    @Bean
    public FeeStructureApi feeStructureApi() {
        return new FeeStructureApi(apiClient);
    }

    @Bean
    public ProductBundleApi productBundleApi() {
        return new ProductBundleApi(apiClient);
    }

    @Bean
    public ProductFeeComponentApi productFeeComponentApi() {
        return new ProductFeeComponentApi(apiClient);
    }

    @Bean
    public FeeApplicationRuleApi feeApplicationRuleApi() {
        return new FeeApplicationRuleApi(apiClient);
    }

    @Bean
    public ProductSubtypeApi productSubtypeApi() {
        return new ProductSubtypeApi(apiClient);
    }

    @Bean
    public ProductApi productApi() {
        return new ProductApi(apiClient);
    }

    @Bean
    public ProductBundleItemApi productBundleItemApi() {
        return new ProductBundleItemApi(apiClient);
    }

    @Bean
    public ProductPricingApi productPricingApi() {
        return new ProductPricingApi(apiClient);
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
    public ProductFeatureApi productFeatureApi() {
        return new ProductFeatureApi(apiClient);
    }

    @Bean
    public ProductLifecycleApi productLifecycleApi() {
        return new ProductLifecycleApi(apiClient);
    }

    @Bean
    public ProductLimitApi productLimitApi() {
        return new ProductLimitApi(apiClient);
    }

    @Bean
    public ProductLocalizationApi productLocalizationApi() {
        return new ProductLocalizationApi(apiClient);
    }

    @Bean
    public ProductVersionApi productVersionApi() {
        return new ProductVersionApi(apiClient);
    }

    @Bean
    public ProductPricingLocalizationApi productPricingLocalizationApi() {
        return new ProductPricingLocalizationApi(apiClient);
    }

}

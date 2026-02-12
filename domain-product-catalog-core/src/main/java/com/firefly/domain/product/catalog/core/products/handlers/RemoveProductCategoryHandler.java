package com.firefly.domain.product.catalog.core.products.handlers;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.command.CommandHandler;
import com.firefly.core.product.sdk.api.ProductCategoryApi;
import com.firefly.domain.product.catalog.core.products.commands.RemoveProductCategoryCommand;
import reactor.core.publisher.Mono;

@CommandHandlerComponent
public class RemoveProductCategoryHandler extends CommandHandler<RemoveProductCategoryCommand, Void> {

    private final ProductCategoryApi productCategoryApi;

    public RemoveProductCategoryHandler(ProductCategoryApi productCategoryApi) {
        this.productCategoryApi = productCategoryApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveProductCategoryCommand cmd) {
        return productCategoryApi.deleteCategory(cmd.productCategoryId()).then();
    }
}

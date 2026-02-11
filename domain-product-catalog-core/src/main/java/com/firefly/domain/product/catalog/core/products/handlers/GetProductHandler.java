package com.firefly.domain.product.catalog.core.products.handlers;

import com.firefly.common.cqrs.annotations.CommandHandlerComponent;
import com.firefly.common.cqrs.annotations.QueryHandlerComponent;
import com.firefly.common.cqrs.command.CommandHandler;
import com.firefly.common.cqrs.query.QueryHandler;
import com.firefly.common.product.sdk.api.ProductApi;
import com.firefly.common.product.sdk.model.ProductDTO;
import com.firefly.domain.product.catalog.core.products.commands.UpdateProductInfoCommand;
import com.firefly.domain.product.catalog.core.products.queries.ProductQuery;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@QueryHandlerComponent
public class GetProductHandler extends QueryHandler<ProductQuery, ProductDTO> {

    private final ProductApi productApi;

    public GetProductHandler(ProductApi productApi) {
        this.productApi = productApi;
    }

    @Override
    protected Mono<ProductDTO> doHandle(ProductQuery cmd) {
        return productApi.getProduct(cmd.getProductId());
    }
}
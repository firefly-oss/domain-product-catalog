package com.firefly.domain.product.catalog.core.products.handlers;

import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.annotations.QueryHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import org.fireflyframework.cqrs.query.QueryHandler;
import com.firefly.core.product.sdk.api.ProductApi;
import com.firefly.core.product.sdk.model.ProductDTO;
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
        return productApi.getProductById(cmd.getProductId(), null);
    }
}
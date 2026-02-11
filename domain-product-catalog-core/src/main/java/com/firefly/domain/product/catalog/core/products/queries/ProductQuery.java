package com.firefly.domain.product.catalog.core.products.queries;

import com.firefly.common.cqrs.query.Query;
import com.firefly.common.product.sdk.model.ProductDTO;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
public class ProductQuery extends ProductDTO implements Query<ProductDTO> {
    private UUID productId;
}
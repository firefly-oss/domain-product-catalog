package com.firefly.domain.product.catalog.core.products.workflows;

import com.firefly.common.cqrs.query.QueryBus;
import com.firefly.core.product.sdk.model.ProductDTO;
import com.firefly.domain.product.catalog.core.products.queries.ProductQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import com.firefly.transactional.saga.annotations.Saga;
import com.firefly.transactional.saga.annotations.SagaStep;
import com.firefly.transactional.saga.annotations.StepEvent;
import com.firefly.transactional.saga.core.SagaContext;

import static com.firefly.domain.product.catalog.core.utils.constants.GlobalConstants.*;
import static com.firefly.domain.product.catalog.core.utils.constants.RegisterProductConstants.*;

@Saga(name = SAGA_GET_PRODUCT_INFO)
@Service
public class GetProductInfoSaga {

    private final QueryBus queryBus;

    @Autowired
    public GetProductInfoSaga(QueryBus queryBus) {
        this.queryBus = queryBus;
    }

    @SagaStep(id = STEP_GET_PRODUCT_INFO)
    @StepEvent(type = EVENT_PRODUCT_INFO_RETRIEVED)
    public Mono<ProductDTO> getProductInfo(ProductQuery query, SagaContext ctx) {
        return queryBus.query(query);
    }
}
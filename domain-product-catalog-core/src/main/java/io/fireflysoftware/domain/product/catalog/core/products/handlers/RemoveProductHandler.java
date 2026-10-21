package io.fireflysoftware.domain.product.catalog.core.products.handlers;

import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import io.fireflysoftware.core.product.sdk.api.ProductApi;
import io.fireflysoftware.core.product.sdk.model.ProductDTO;
import io.fireflysoftware.domain.product.catalog.core.products.commands.RemoveProductCommand;
import reactor.core.publisher.Mono;
import java.util.UUID;

@CommandHandlerComponent
public class RemoveProductHandler extends CommandHandler<RemoveProductCommand, Void> {

    private final ProductApi productApi;

    public RemoveProductHandler(ProductApi productApi) {
        this.productApi = productApi;
    }

    @Override
    protected Mono<Void> doHandle(RemoveProductCommand cmd) {
        return productApi.getProductById(cmd.productId(), UUID.randomUUID().toString())
                .flatMap(product -> {
                    ProductDTO.ProductStatusEnum status = product.getProductStatus();
                    if (status != ProductDTO.ProductStatusEnum.DRAFT
                            && status != ProductDTO.ProductStatusEnum.PROPOSED) {
                        return Mono.error(new IllegalStateException(
                                "Product " + cmd.productId() + " is " + status
                                        + " and cannot be deleted; retire it so agreements that reference it keep a resolvable definition"));
                    }
                    return productApi.deleteProduct(cmd.productId(), UUID.randomUUID().toString());
                })
                .then();
    }
}
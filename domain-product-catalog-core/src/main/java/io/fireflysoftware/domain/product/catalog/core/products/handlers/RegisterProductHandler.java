package io.fireflysoftware.domain.product.catalog.core.products.handlers;

import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import io.fireflysoftware.core.product.sdk.api.ProductApi;
import io.fireflysoftware.domain.product.catalog.core.products.commands.RegisterProductCommand;
import io.fireflysoftware.domain.product.catalog.core.products.commands.RegisterProductInfoCommand;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@CommandHandlerComponent
public class RegisterProductHandler extends CommandHandler<RegisterProductInfoCommand, UUID> {

    private final ProductApi productApi;

    public RegisterProductHandler(ProductApi productApi) {
        this.productApi = productApi;
    }

    @Override
    protected Mono<UUID> doHandle(RegisterProductInfoCommand cmd) {
        return productApi.createProduct(cmd, UUID.randomUUID().toString())
                .mapNotNull(productDTO ->
                        Objects.requireNonNull(Objects.requireNonNull(productDTO)).getProductId());
    }
}
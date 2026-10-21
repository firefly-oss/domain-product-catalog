package io.fireflysoftware.domain.product.catalog.core.products.commands;

import io.fireflysoftware.core.product.sdk.model.ProductConfigurationDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.fireflyframework.cqrs.command.Command;

import java.util.UUID;

/**
 * One product_configuration row. Loan amount, rate brackets, and fees are optional keys.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class RegisterProductConfigurationCommand extends ProductConfigurationDTO implements Command<UUID> {

    public RegisterProductConfigurationCommand withProductId(UUID productId) {
        this.setProductId(productId);
        return this;
    }
}

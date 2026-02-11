package com.firefly.domain.product.catalog.core.products.commands;

import com.firefly.common.cqrs.command.Command;
import com.firefly.common.product.sdk.model.ProductDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
public class RegisterProductInfoCommand extends ProductDTO implements Command<UUID> {
    private UUID productSubtypeId;


    public RegisterProductInfoCommand withProductSubtypeId(UUID productSubtypeId) {
        this.setProductSubtypeId(productSubtypeId);
        return this;
    }
}

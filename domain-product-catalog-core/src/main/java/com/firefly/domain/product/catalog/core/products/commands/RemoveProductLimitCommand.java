package com.firefly.domain.product.catalog.core.products.commands;

import com.firefly.common.cqrs.command.Command;

import java.util.UUID;

public record RemoveProductLimitCommand(
        UUID productId,
        UUID productLimitId
) implements Command<Void>{}
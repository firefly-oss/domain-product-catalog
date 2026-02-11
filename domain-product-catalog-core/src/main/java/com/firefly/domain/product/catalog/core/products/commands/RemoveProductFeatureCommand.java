package com.firefly.domain.product.catalog.core.products.commands;

import com.firefly.common.cqrs.command.Command;

import java.util.UUID;

public record RemoveProductFeatureCommand(
        UUID productId,
        UUID productFeatureId
) implements Command<Void>{}
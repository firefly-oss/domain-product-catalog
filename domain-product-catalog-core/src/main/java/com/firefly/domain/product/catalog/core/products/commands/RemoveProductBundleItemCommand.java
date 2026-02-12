package com.firefly.domain.product.catalog.core.products.commands;

import org.fireflyframework.cqrs.command.Command;

import java.util.UUID;

public record RemoveProductBundleItemCommand(
        UUID productId,
        UUID productBundleId,
        UUID productBundleItemId
) implements Command<Void>{}

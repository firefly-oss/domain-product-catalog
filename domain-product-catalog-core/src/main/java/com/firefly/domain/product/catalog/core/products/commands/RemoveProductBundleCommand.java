package com.firefly.domain.product.catalog.core.products.commands;

import org.fireflyframework.cqrs.command.Command;

import java.util.UUID;

public record RemoveProductBundleCommand(
        UUID productId,
        UUID productBundleId
) implements Command<Void>{}

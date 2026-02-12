package com.firefly.domain.product.catalog.core.products.commands;

import org.fireflyframework.cqrs.command.Command;

import java.util.UUID;

public record RemoveProductVersionCommand(
        UUID productId,
        UUID productVersionId
) implements Command<Void>{}
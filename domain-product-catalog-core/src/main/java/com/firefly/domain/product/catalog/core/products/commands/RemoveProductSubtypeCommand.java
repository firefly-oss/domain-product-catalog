package com.firefly.domain.product.catalog.core.products.commands;

import org.fireflyframework.cqrs.command.Command;

import java.util.UUID;

public record RemoveProductSubtypeCommand(
        UUID productId,
        UUID productCategoryId,
        UUID productSubtypeId
) implements Command<Void>{}

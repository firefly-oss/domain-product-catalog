package com.firefly.domain.product.catalog.core.products.commands;

import org.fireflyframework.cqrs.command.Command;

import java.util.UUID;

public record RemoveProductCategoryCommand(
        UUID productCategoryId
) implements Command<Void>{}

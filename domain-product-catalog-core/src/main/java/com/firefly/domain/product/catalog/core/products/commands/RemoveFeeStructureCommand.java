package com.firefly.domain.product.catalog.core.products.commands;

import org.fireflyframework.cqrs.command.Command;

import java.util.UUID;

public record RemoveFeeStructureCommand(
        UUID productId,
        UUID feeStructureId
) implements Command<Void>{}

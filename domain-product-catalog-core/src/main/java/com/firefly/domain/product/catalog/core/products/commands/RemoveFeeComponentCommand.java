package com.firefly.domain.product.catalog.core.products.commands;

import com.firefly.common.cqrs.command.Command;

import java.util.UUID;

public record RemoveFeeComponentCommand(
        UUID feeComponentId,
        UUID feeStructureId
) implements Command<Void>{}
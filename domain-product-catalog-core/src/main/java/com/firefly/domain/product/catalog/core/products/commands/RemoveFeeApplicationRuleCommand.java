package com.firefly.domain.product.catalog.core.products.commands;

import org.fireflyframework.cqrs.command.Command;

import java.util.UUID;

public record RemoveFeeApplicationRuleCommand(
        UUID productId,
        UUID feeApplicationRuleId,
        UUID feeStructureId,
        UUID componentId
) implements Command<Void>{}

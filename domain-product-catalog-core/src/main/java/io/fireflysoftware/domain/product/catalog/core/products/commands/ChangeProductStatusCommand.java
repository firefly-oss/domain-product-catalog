package io.fireflysoftware.domain.product.catalog.core.products.commands;

import java.util.UUID;

/**
 * Saga input for a product status change. Not dispatched on the command bus.
 */
public record ChangeProductStatusCommand(UUID productId, String status) {
}

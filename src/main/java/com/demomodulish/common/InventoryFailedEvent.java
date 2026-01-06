package com.demomodulish.common;

public record InventoryFailedEvent(
        String orderId,
        String reason
) {
}

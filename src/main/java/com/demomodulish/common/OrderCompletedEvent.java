package com.demomodulish.common;

public record OrderCompletedEvent(String orderId, String productId, int quantity) {}

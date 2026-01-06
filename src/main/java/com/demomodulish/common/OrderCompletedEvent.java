package com.demomodulish.common;

public record OrderCompletedEvent(
        String orderId,
        String productId,
        int quantity,
        long totalAmount // 총 주문 금액 필드 추가
) {}

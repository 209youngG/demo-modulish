package com.demomodulish.common;

import java.util.Map;

public record PaymentFailedEvent(
        String orderId,
        String reason,
        String productId,
        int quantity,
        Map<String, Integer> deductedBatches // 차감된 배치 정보 추가
) {
}

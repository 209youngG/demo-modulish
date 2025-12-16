package com.demomodulish.order;

public enum OrderStatus {
    PENDING,   // 주문 대기 (재고 확인 전)
    COMPLETED, // 주문 완료 (재고 차감 성공 - 필요시 추가 구현)
    CANCELLED  // 주문 취소 (재고 부족)
}

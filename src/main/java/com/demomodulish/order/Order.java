package com.demomodulish.order;

import jakarta.persistence.*;
import lombok.Getter;

import java.util.UUID;

@Getter
@Entity
@Table(name = "orders")
public class Order {

    @Id
    private String id = UUID.randomUUID().toString();
    private String productId;
    private int quantity;

    @Enumerated(EnumType.STRING)
    private OrderStatus status; // 상태 필드 추가

    protected Order() {}

    public Order(String productId, int quantity) {
        this.productId = productId;
        this.quantity = quantity;
        this.status = OrderStatus.PENDING; // 기본값은 대기
    }

    public void cancel() {
        this.status = OrderStatus.CANCELLED;
    }

    public void complete() {
        this.status = OrderStatus.COMPLETED;
    }
}

package com.demomodulish.order;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

import java.util.UUID;

@Getter
@Entity
@Table(name = "orders")
public class Order {

    @Id
    private String id = UUID.randomUUID().toString();

    @NotBlank
    private String productId;
    
    @Min(1)
    private int quantity;
    
    @Min(0)
    private long price;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    protected Order() {}

    public Order(String productId, int quantity, long price) {
        this.productId = productId;
        this.quantity = quantity;
        this.price = price;
        this.status = OrderStatus.PENDING;
    }

    public long getTotalAmount() {
        return this.price * this.quantity;
    }

    public void cancel() {
        this.status = OrderStatus.CANCELLED;
    }

    public void complete() {
        this.status = OrderStatus.COMPLETED;
    }
}

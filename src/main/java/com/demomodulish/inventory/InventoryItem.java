package com.demomodulish.inventory;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
public class InventoryItem {

    @Id
    private String id = UUID.randomUUID().toString();
    private String productId;
    private int quantity;
    private LocalDateTime expirationDate;

    public InventoryItem(String productId, int quantity, LocalDateTime expirationDate) {
        this.productId = productId;
        this.quantity = quantity;
        this.expirationDate = expirationDate;
    }

    protected InventoryItem() {}

    public int decrease(int amount) {
        int deducated = Math.min(this.quantity, amount);
        this.quantity -= deducated;
        return deducated;
    }

    public void increase(int amount) {
        this.quantity += amount;
    }
}

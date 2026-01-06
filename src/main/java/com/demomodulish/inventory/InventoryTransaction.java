package com.demomodulish.inventory;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
class InventoryTransaction {
    @Id
    private String orderId;
    private LocalDateTime processedAt;
}

package com.demomodulish.inventory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InventoryItem 엔티티 단위 테스트")
class InventoryItemTest {

    private int getQuantity(InventoryItem item) {
        try {
            Field field = InventoryItem.class.getDeclaredField("quantity");
            field.setAccessible(true);
            return (int) field.get(item);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("재고 감소 시 요청 수량보다 많으면 전체 수량을 반환한다")
    void shouldReturnAllWhenDecreaseMoreThanAvailable() {
        InventoryItem item = new InventoryItem("PRODUCT-1", 5, LocalDateTime.now().plusDays(10));

        int deducted = item.decrease(10);

        assertThat(deducted).isEqualTo(5);
        assertThat(getQuantity(item)).isZero();
    }

    @Test
    @DisplayName("재고 감소 시 요청 수량보다 적으면 부족한 만큼만 반환한다")
    void shouldReturnPartialWhenDecreaseLessThanAvailable() {
        InventoryItem item = new InventoryItem("PRODUCT-1", 10, LocalDateTime.now().plusDays(10));

        int deducted = item.decrease(3);

        assertThat(deducted).isEqualTo(3);
        assertThat(getQuantity(item)).isEqualTo(7);
    }

    @Test
    @DisplayName("재고 증가 시 수량이 늘어난다")
    void shouldIncreaseQuantity() {
        InventoryItem item = new InventoryItem("PRODUCT-1", 5, LocalDateTime.now().plusDays(10));

        item.increase(3);

        assertThat(getQuantity(item)).isEqualTo(8);
    }

    @Test
    @DisplayName("유효한 재고 항목을 생성한다")
    void shouldCreateValidInventoryItem() {
        String productId = "PRODUCT-1";
        int quantity = 10;
        LocalDateTime expiration = LocalDateTime.now().plusDays(7);

        InventoryItem item = new InventoryItem(productId, quantity, expiration);

        assertThat(item.getProductId()).isEqualTo(productId);
        assertThat(getQuantity(item)).isEqualTo(quantity);
        assertThat(item.getExpirationDate()).isEqualTo(expiration);
        assertThat(item.getId()).isNotNull();
    }
}

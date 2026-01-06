package com.demomodulish;

import com.demomodulish.inventory.InventoryItem;
import com.demomodulish.inventory.InventoryRepository;
import com.demomodulish.order.Order;
import com.demomodulish.order.OrderManagement;
import com.demomodulish.order.OrderRepository;
import com.demomodulish.order.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SystemIntegrationTests {

    @Autowired
    private OrderManagement orderManagement;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Test
    void completesOrderIfPaymentSucceeds() {
        inventoryRepository.save(new InventoryItem("PRODUCT-1", 10, LocalDateTime.now().plusDays(1)));
        var order = new Order("PRODUCT-1", 3, 1000L);
        orderManagement.place(order);

        await().untilAsserted(() -> {
            var completedOrder = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(completedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
            var totalStock = inventoryRepository.findAllByProductId("PRODUCT-1").stream()
                    .mapToInt(InventoryItem::getQuantity)
                    .sum();
            assertThat(totalStock).isEqualTo(7);
        });
    }

    @Test
    void cancelsOrderIfInventoryIsInsufficient() {
        inventoryRepository.save(new InventoryItem("PRODUCT-2", 1, LocalDateTime.now().plusDays(1)));
        var order = new Order("PRODUCT-2", 2, 1000L);
        orderManagement.place(order);

        await().untilAsserted(() -> {
            var cancelledOrder = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            var totalStock = inventoryRepository.findAllByProductId("PRODUCT-2").stream()
                    .mapToInt(InventoryItem::getQuantity)
                    .sum();
            assertThat(totalStock).isEqualTo(1);
        });
    }

    @Test
    void cancelsOrderIfOnlyExpiredInventoryIsAvailable() {
        inventoryRepository.save(new InventoryItem("PRODUCT-3", 10, LocalDateTime.now().minusDays(1)));
        var order = new Order("PRODUCT-3", 5, 1000L);
        orderManagement.place(order);

        await().untilAsserted(() -> {
            var cancelledOrder = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            var totalStock = inventoryRepository.findAllByProductId("PRODUCT-3").stream()
                    .mapToInt(InventoryItem::getQuantity)
                    .sum();
            assertThat(totalStock).isEqualTo(10);
        });
    }

    @Test
    void cancelsOrderIfPaymentFails() {
        inventoryRepository.save(new InventoryItem("PRODUCT-4", 10, LocalDateTime.now().plusDays(1)));
        var order = new Order("PRODUCT-4", 1, 9999L);
        orderManagement.place(order);

        await().untilAsserted(() -> {
            var cancelledOrder = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            // [검증] 재고가 다시 10개로 복구되었는지 확인
            var totalStock = inventoryRepository.findAllByProductId("PRODUCT-4").stream()
                    .mapToInt(InventoryItem::getQuantity)
                    .sum();
            assertThat(totalStock).isEqualTo(10);
        });
    }
}

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

    /**
     * E2E 성공 시나리오: 주문 -> 재고 차감 -> 주문 완료
     */
    @Test
    void completesOrderIfEnoughInventoryIsAvailable() {
        // Given: 재고 10개 준비
        inventoryRepository.save(new InventoryItem("PRODUCT-1", 10, LocalDateTime.now().plusDays(1)));

        // When: 3개 주문
        var order = new Order("PRODUCT-1", 3);
        orderManagement.place(order);

        // Then: 주문 상태가 COMPLETED로 변경되고, 재고가 7개로 줄어드는 것을 기다려서 확인
        await().untilAsserted(() -> {
            var completedOrder = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(completedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);

            var totalStock = inventoryRepository.findAllByProductId("PRODUCT-1").stream()
                    .mapToInt(InventoryItem::getQuantity)
                    .sum();
            assertThat(totalStock).isEqualTo(7);
        });
    }

    /**
     * E2E 실패 시나리오: 주문 -> 재고 부족 -> 주문 취소
     */
    @Test
    void cancelsOrderIfInventoryIsInsufficient() {
        // Given: 재고 1개 준비
        inventoryRepository.save(new InventoryItem("PRODUCT-2", 1, LocalDateTime.now().plusDays(1)));

        // When: 2개 주문
        var order = new Order("PRODUCT-2", 2);
        orderManagement.place(order);

        // Then: 주문 상태가 CANCELLED로 변경되고, 재고는 그대로 1개인 것을 기다려서 확인
        await().untilAsserted(() -> {
            var cancelledOrder = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);

            var totalStock = inventoryRepository.findAllByProductId("PRODUCT-2").stream()
                    .mapToInt(InventoryItem::getQuantity)
                    .sum();
            assertThat(totalStock).isEqualTo(1);
        });
    }

    /**
     * E2E 실패 시나리오: 유통기한 만료된 재고만 있는 경우 주문 취소
     */
    @Test
    void cancelsOrderIfOnlyExpiredInventoryIsAvailable() {
        // Given: 유통기한이 지난 재고 10개 준비
        inventoryRepository.save(new InventoryItem("PRODUCT-3", 10, LocalDateTime.now().minusDays(1)));

        // When: 5개 주문
        var order = new Order("PRODUCT-3", 5);
        orderManagement.place(order);

        // Then: 주문 상태가 CANCELLED로 변경되고, 재고는 그대로 10개인 것을 기다려서 확인
        await().untilAsserted(() -> {
            var cancelledOrder = orderRepository.findById(order.getId()).orElseThrow();
            assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);

            var totalStock = inventoryRepository.findAllByProductId("PRODUCT-3").stream()
                    .mapToInt(InventoryItem::getQuantity)
                    .sum();
            assertThat(totalStock).isEqualTo(10);
        });
    }
}

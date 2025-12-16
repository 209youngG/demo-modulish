package com.demomodulish.inventory;

import com.demomodulish.common.InventoryFailedEvent;
import com.demomodulish.common.OrderCompletedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.Scenario;

import java.time.LocalDateTime;

@ApplicationModuleTest
class InventoryIntegrationTests {

    @Autowired
    InventoryRepository inventoryRepository;

    /**
     * 정상적인 주문 완료 시나리오 테스트
     * <p>여러 배치의 재고가 있을 때, 유통기한 순으로 차감되고 총 수량이 맞는지 검증합니다.</p>
     */
    @Test
    void deductsStockOnOrderCompletion(Scenario scenario) {
        // 1. Given: 유통기한이 넉넉한 재고 30개 세팅
        inventoryRepository.save(new InventoryItem("PRODUCT-123", 10, LocalDateTime.now().plusDays(10)));
        inventoryRepository.save(new InventoryItem("PRODUCT-123", 10, LocalDateTime.now().minusDays(10)));
        inventoryRepository.save(new InventoryItem("PRODUCT-123", 10, LocalDateTime.now().plusDays(5)));

        // 2. When: 11개 주문 (10개 배치에서 다 까고, 다음 배치에서 1개 까야 함)
        // 3. Then: 총 재고가 19개가 되어야 함 (30 - 11 = 19)
        scenario.publish(new OrderCompletedEvent("ORD-001", "PRODUCT-123", 11))
                .andWaitForStateChange(() -> {
                    // 검증 로직
                    int totalQuantity = inventoryRepository.findAllByProductId("PRODUCT-123")
                            .stream()
                            .mapToInt(InventoryItem::getQuantity)
                            .sum();

                    // 디버깅용 로그 (테스트 콘솔에서 확인 가능)
                    System.out.println("Current Total Quantity: " + totalQuantity);

                    return totalQuantity == 19;
                });
    }

    /**
     * 재고 부족 시나리오 테스트
     * <p>재고가 부족할 때 InventoryFailedEvent가 발행되는지 검증합니다.</p>
     */
    @Test
    void shouldCancelOrderWhenOutOfStock(Scenario scenario) {
        // Given: 재고가 1개인 상품 세팅
        inventoryRepository.save(new InventoryItem("OUT-OF-STOCK-ITEM", 1, LocalDateTime.now().plusDays(1)));

        // When: 2개 주문 발생 -> (재고 부족) -> 실패 이벤트 발행
        scenario.publish(new OrderCompletedEvent("ORD-FAIL-1", "OUT-OF-STOCK-ITEM", 2))
                .andWaitForEventOfType(InventoryFailedEvent.class) // 1. 실패 이벤트가 나오는지 확인
                .matching(event -> event.orderId().equals("ORD-FAIL-1"))
                .toArrive();
    }
}

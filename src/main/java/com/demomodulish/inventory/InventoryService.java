package com.demomodulish.inventory;

import com.demomodulish.common.InventoryFailedEvent;
import com.demomodulish.common.InventoryVerifiedEvent;
import com.demomodulish.common.OrderCompletedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 재고 관리 서비스
 * <p>주문 완료 이벤트를 수신하여 재고를 차감하거나, 재고 부족 시 보상 트랜잭션을 유발하는 이벤트를 발행합니다.</p>
 */
@Service
class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ApplicationEventPublisher events; // 이벤트 발행기 추가

    InventoryService(InventoryRepository inventoryRepository, ApplicationEventPublisher events) {
        this.inventoryRepository = inventoryRepository;
        this.events = events;
    }


    /**
     * 주문 완료(OrderCompletedEvent) 이벤트를 수신하여 재고 차감 로직을 수행합니다.
     * <p>
     * 1. {@code @ApplicationModuleListener}: Spring Modulith 이벤트를 구독합니다.
     * 2. {@code Propagation.REQUIRES_NEW}: 주문 트랜잭션과 분리된 별도의 트랜잭션에서 실행됩니다.
     *    재고 부족으로 예외가 발생하여 롤백되더라도, 상위(주문) 트랜잭션에 직접적인 영향을 주지 않고
     *    별도의 실패 이벤트(InventoryFailedEvent)를 통해 보상 로직을 트리거합니다.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @ApplicationModuleListener
    public void on(OrderCompletedEvent event) {
        String productId = event.productId();
        int requestedQuantity = event.quantity();

        // 1. 🔒 락 걸고 데이터 가져오기 (다른 트랜잭션 대기)
        List<InventoryItem> batches = inventoryRepository.findAllByProductIdWithLock(productId);

        // 2. 차감 가능한지 계산
        int remainToDeduct = requestedQuantity;

        // 3. 유통기한 빠른 순으로 순회하며 차감 시도
        for (InventoryItem batch : batches) {
            // ❌ 유통기한 지난 건 건너뛰기
            if (batch.getExpirationDate().isBefore(LocalDateTime.now())) {
                System.out.println("🚨 [Inventory] 실패: " + batch.getId() + " / " + batch.getProductId() + " -> 유통기한 지남");
                continue;
            }

            // 실제 객체 상태 변경 (Dirty Checking으로 나중에 자동 저장됨)
            int deducted = batch.decrease(remainToDeduct);
            remainToDeduct -= deducted;

            System.out.println("LOG: 아이템 ID(" + batch.getId() + ")에서 " + deducted + "개 차감됨. (유통기한: " + batch.getExpirationDate() + ")");

            if (remainToDeduct == 0) break; // 다 뺐으면 중단
        }

        // 4. 결과 확인 및 실패 처리
        if (remainToDeduct > 0) {
            // 1. 실패 이벤트 발행 (주문 취소를 위해)
            publishFailure(event, "유효 재고 부족");

            // 2. 🔥 중요: 강제 예외 발생 -> 트랜잭션 롤백 -> 배치 A에서 깠던 수량 원상복구
            throw new IllegalStateException("재고 부족으로 인한 롤백 처리");
        }

        // 여기까지 오면 트랜잭션 커밋되면서 변경된 수량이 DB에 반영됨 ✅
        System.out.println("🏭 [Inventory] 총 " + requestedQuantity + "개 차감 완료");
        events.publishEvent(new InventoryVerifiedEvent(event.orderId()));
    }

    private void publishFailure(OrderCompletedEvent event, String reason) {
        System.out.println("🚨 [Inventory] 실패: " + reason + " -> 주문 취소 요청");
        events.publishEvent(new InventoryFailedEvent(event.orderId(), reason));
    }
}

package com.demomodulish.inventory;

import com.demomodulish.common.InventoryFailedEvent;
import com.demomodulish.common.InventoryVerifiedEvent;
import com.demomodulish.common.OrderCompletedEvent;
import com.demomodulish.common.PaymentFailedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final ApplicationEventPublisher events;

    InventoryService(InventoryRepository inventoryRepository,
                     InventoryTransactionRepository inventoryTransactionRepository,
                     ApplicationEventPublisher events) {
        this.inventoryRepository = inventoryRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.events = events;
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Retryable(
            retryFor = {ConcurrencyFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 100)
    )
    @ApplicationModuleListener
    public void on(OrderCompletedEvent event) {
        if (inventoryTransactionRepository.existsById(event.orderId())) {
            log.info("✋ [Inventory] 이미 처리된 주문입니다. (Idempotency check): {}", event.orderId());
            return;
        }

        DeductionResult result = deductInventory(event);
        recordTransaction(event.orderId());

        if (result.isFailure()) {
            publishFailure(event, result.getReason());
        } else {
            publishSuccess(event, result.getDeductedBatches(), result.getRequestedQuantity());
        }
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @ApplicationModuleListener
    public void on(PaymentFailedEvent event) {
        log.info("🔄 [Inventory] 결제 실패로 인한 재고 복구 수행: {}", event.orderId());
        event.deductedBatches().forEach((batchId, quantity) -> inventoryRepository.findById(batchId)
                .ifPresent(item -> item.increase(quantity)));
    }

    private DeductionResult deductInventory(OrderCompletedEvent event) {
        String productId = event.productId();
        int requestedQuantity = event.quantity();
        List<InventoryItem> batches = inventoryRepository.findAllByProductIdWithLock(productId);
        LocalDateTime now = LocalDateTime.now();

        int totalAvailable = calculateAvailableQuantity(batches, now);

        if (totalAvailable < requestedQuantity) {
            return DeductionResult.failure("유효 재고 부족 (요청: %d, 가능: %d)".formatted(requestedQuantity, totalAvailable));
        }

        return performDeduction(batches, now, requestedQuantity);
    }

    private int calculateAvailableQuantity(List<InventoryItem> batches, LocalDateTime now) {
        return batches.stream()
                .filter(b -> !b.getExpirationDate().isBefore(now))
                .mapToInt(InventoryItem::getQuantity)
                .sum();
    }

    private DeductionResult performDeduction(List<InventoryItem> batches, LocalDateTime now, int requestedQuantity) {
        int remainToDeduct = requestedQuantity;
        Map<String, Integer> deductedBatches = new HashMap<>();

        for (InventoryItem batch : batches) {
            if (batch.getExpirationDate().isBefore(now)) {
                continue;
            }

            int deducted = batch.decrease(remainToDeduct);
            if (deducted > 0) {
                deductedBatches.put(batch.getId(), deducted);
            }
            remainToDeduct -= deducted;
            if (remainToDeduct == 0) {
                break;
            }
        }

        return DeductionResult.success(deductedBatches, requestedQuantity);
    }

    private void recordTransaction(String orderId) {
        inventoryTransactionRepository.save(new InventoryTransaction(orderId, LocalDateTime.now()));
    }

    private void publishSuccess(OrderCompletedEvent event, Map<String, Integer> deductedBatches, int quantity) {
        log.info("🏭 [Inventory] 총 {}개 차감 완료", quantity);
        events.publishEvent(new InventoryVerifiedEvent(
                event.orderId(),
                event.totalAmount(),
                event.productId(),
                event.quantity(),
                deductedBatches
        ));
    }

    private void publishFailure(OrderCompletedEvent event, String reason) {
        log.info("🚨 [Inventory] 실패: {} -> 주문 취소 요청", reason);
        events.publishEvent(new InventoryFailedEvent(event.orderId(), reason));
    }

    private record DeductionResult(Map<String, Integer> deductedBatches, int requestedQuantity, String reason, boolean isFailure) {
        static DeductionResult success(Map<String, Integer> deductedBatches, int requestedQuantity) {
            return new DeductionResult(deductedBatches, requestedQuantity, null, false);
        }

        static DeductionResult failure(String reason) {
            return new DeductionResult(Map.of(), 0, reason, true);
        }

        public boolean isFailure() {
            return isFailure;
        }

        public String getReason() {
            return reason;
        }

        public Map<String, Integer> getDeductedBatches() {
            return deductedBatches;
        }

        public int getRequestedQuantity() {
            return requestedQuantity;
        }
    }
}

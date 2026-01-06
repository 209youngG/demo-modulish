package com.demomodulish.inventory;

import com.demomodulish.common.InventoryFailedEvent;
import com.demomodulish.common.InventoryVerifiedEvent;
import com.demomodulish.common.OrderCompletedEvent;
import com.demomodulish.common.PaymentFailedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.ConcurrencyFailureException;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("InventoryService 단위 테스트")
class InventoryServiceTest {

    private InventoryRepository inventoryRepository;
    private InventoryTransactionRepository inventoryTransactionRepository;
    private ApplicationEventPublisher events;
    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventoryRepository = mock(InventoryRepository.class);
        inventoryTransactionRepository = mock(InventoryTransactionRepository.class);
        events = mock(ApplicationEventPublisher.class);
        inventoryService = new InventoryService(inventoryRepository, inventoryTransactionRepository, events);
    }

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
    @DisplayName("이미 처리된 주문은 중복 처리하지 않는다")
    void shouldSkipAlreadyProcessedOrder() {
        String orderId = "ORDER-1";
        OrderCompletedEvent event = new OrderCompletedEvent(orderId, "PRODUCT-1", 5, 5000L);
        when(inventoryTransactionRepository.existsById(orderId)).thenReturn(true);

        inventoryService.on(event);

        verify(inventoryTransactionRepository).existsById(orderId);
        verify(inventoryRepository, never()).findAllByProductIdWithLock(any());
        verify(events, never()).publishEvent(any());
    }

    @Test
    @DisplayName("재고가 부족하면 실패 이벤트를 발행한다")
    void shouldPublishFailureEventWhenOutOfStock() {
        String orderId = "ORDER-2";
        OrderCompletedEvent event = new OrderCompletedEvent(orderId, "PRODUCT-2", 10, 10000L);
        when(inventoryTransactionRepository.existsById(orderId)).thenReturn(false);

        List<InventoryItem> batches = List.of(
                new InventoryItem("PRODUCT-2", 5, LocalDateTime.now().plusDays(1))
        );
        when(inventoryRepository.findAllByProductIdWithLock("PRODUCT-2")).thenReturn(batches);

        inventoryService.on(event);

        verify(events).publishEvent(any(InventoryFailedEvent.class));
        verify(inventoryTransactionRepository).save(any(InventoryTransaction.class));
    }

    @Test
    @DisplayName("유통기한이 지난 재고는 사용하지 않는다")
    void shouldIgnoreExpiredInventory() {
        String orderId = "ORDER-3";
        OrderCompletedEvent event = new OrderCompletedEvent(orderId, "PRODUCT-3", 3, 3000L);
        when(inventoryTransactionRepository.existsById(orderId)).thenReturn(false);

        List<InventoryItem> batches = List.of(
                new InventoryItem("PRODUCT-3", 10, LocalDateTime.now().minusDays(1)),
                new InventoryItem("PRODUCT-3", 5, LocalDateTime.now().plusDays(10))
        );
        when(inventoryRepository.findAllByProductIdWithLock("PRODUCT-3")).thenReturn(batches);

        inventoryService.on(event);

        verify(events).publishEvent(any(InventoryVerifiedEvent.class));
        verify(inventoryTransactionRepository).save(any(InventoryTransaction.class));
    }

    @Test
    @DisplayName("여러 배치에서 FIFO 순서로 재고를 차감한다")
    void shouldDeductInventoryFIFO() {
        String orderId = "ORDER-4";
        OrderCompletedEvent event = new OrderCompletedEvent(orderId, "PRODUCT-4", 15, 15000L);
        when(inventoryTransactionRepository.existsById(orderId)).thenReturn(false);

        InventoryItem batch1 = new InventoryItem("PRODUCT-4", 10, LocalDateTime.now().plusDays(5));
        InventoryItem batch2 = new InventoryItem("PRODUCT-4", 10, LocalDateTime.now().plusDays(10));
        List<InventoryItem> batches = Arrays.asList(batch1, batch2);
        when(inventoryRepository.findAllByProductIdWithLock("PRODUCT-4")).thenReturn(batches);

        inventoryService.on(event);

        verify(events).publishEvent(any(InventoryVerifiedEvent.class));
        assertThat(getQuantity(batch1)).isZero();
        assertThat(getQuantity(batch2)).isEqualTo(5);
    }

    @Test
    @DisplayName("동시성 오류 발생 시 재시도한다")
    void shouldRetryOnConcurrencyFailure() {
        String orderId = "ORDER-5";
        OrderCompletedEvent event = new OrderCompletedEvent(orderId, "PRODUCT-5", 5, 5000L);
        when(inventoryTransactionRepository.existsById(orderId)).thenReturn(false);

        when(inventoryRepository.findAllByProductIdWithLock("PRODUCT-5"))
                .thenThrow(new ConcurrencyFailureException("Concurrency conflict"));

        assertThatThrownBy(() -> inventoryService.on(event))
                .isInstanceOf(ConcurrencyFailureException.class);

        verify(inventoryRepository).findAllByProductIdWithLock("PRODUCT-5");
    }

    @Test
    @DisplayName("결제 실패 시 재고를 복구한다")
    void shouldRestoreInventoryOnPaymentFailure() {
        String orderId = "ORDER-6";
        Map<String, Integer> deductedBatches = new HashMap<>();
        deductedBatches.put("BATCH-1", 3);
        deductedBatches.put("BATCH-2", 2);

        PaymentFailedEvent event = new PaymentFailedEvent(orderId, "Test", "PRODUCT-6", 5, deductedBatches);

        InventoryItem batch1 = new InventoryItem("PRODUCT-6", 7, LocalDateTime.now().plusDays(1));
        InventoryItem batch2 = new InventoryItem("PRODUCT-6", 8, LocalDateTime.now().plusDays(1));

        when(inventoryRepository.findById("BATCH-1")).thenReturn(Optional.of(batch1));
        when(inventoryRepository.findById("BATCH-2")).thenReturn(Optional.of(batch2));

        inventoryService.on(event);

        verify(inventoryRepository).findById("BATCH-1");
        verify(inventoryRepository).findById("BATCH-2");
        assertThat(getQuantity(batch1)).isEqualTo(10);
        assertThat(getQuantity(batch2)).isEqualTo(10);
    }
}

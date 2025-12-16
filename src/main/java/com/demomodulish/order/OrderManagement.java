package com.demomodulish.order;

import com.demomodulish.common.InventoryFailedEvent;
import com.demomodulish.common.InventoryVerifiedEvent;
import com.demomodulish.common.OrderCompletedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@Transactional
public class OrderManagement {

    private final OrderRepository orders;
    private final ApplicationEventPublisher events;

    public OrderManagement(OrderRepository orders, ApplicationEventPublisher events) {
        this.orders = orders;
        this.events = events;
    }

    public void place(Order order) {
        // 1. DB 저장
        orders.save(order);

        // 2. 이벤트 발행 (트랜잭션 커밋 시점에 리스너들에게 전달됨)
        events.publishEvent(new OrderCompletedEvent(order.getId(), order.getProductId(), order.getQuantity()));
    }

    /**
     * 재고 확인 완료(InventoryVerifiedEvent) 시 실행되는 리스너입니다.
     * <p>
     * {@code @ApplicationModuleListener}는 기본적으로 {@code TransactionPhase.AFTER_COMMIT}에 실행됩니다.
     * 즉, InventoryService의 트랜잭션이 성공적으로 커밋된 후에만 이 메서드가 호출되어 주문을 확정합니다.
     */
    @ApplicationModuleListener
    public void on(InventoryVerifiedEvent event) {
        orders.findById(event.orderId()).ifPresent(order -> {
            System.out.println("✅ [Order] 재고 확인 완료 -> 주문 확정(COMPLETED): " + order.getId());
            order.complete(); // 상태를 COMPLETED로 변경
        });
    }

    /**
     * 재고 부족(InventoryFailedEvent) 시 실행되는 보상 트랜잭션 리스너입니다.
     * <p>
     * 1. {@code phase = TransactionPhase.AFTER_ROLLBACK}: InventoryService 트랜잭션이 예외로 인해 롤백된 후에 실행됩니다.
     * 2. {@code Propagation.REQUIRES_NEW}: 이미 롤백된 트랜잭션 컨텍스트 대신 새로운 트랜잭션을 시작하여 주문 취소 상태를 저장합니다.
     */
    @Async // 주문 취소는 별도 스레드에서 비동기로 실행 (추천)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(InventoryFailedEvent event) {
        orders.findById(event.orderId()).ifPresent(order -> {
            System.out.println("📦 [Order] 재고 부족으로 인한 주문 취소 처리: " + order.getId());
            order.cancel();
        });
    }
}

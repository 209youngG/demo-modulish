package com.demomodulish.order;

import com.demomodulish.common.InventoryFailedEvent;
import com.demomodulish.common.OrderCompletedEvent;
import com.demomodulish.common.PaymentCompletedEvent;
import com.demomodulish.common.PaymentFailedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
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
        orders.save(order);
        // 총 금액을 이벤트에 포함하여 발행
        events.publishEvent(new OrderCompletedEvent(
                order.getId(),
                order.getProductId(),
                order.getQuantity(),
                order.getTotalAmount()
        ));
    }

    /**
     * [변경] 결제 완료 시 주문을 최종 확정합니다.
     */
    @ApplicationModuleListener
    public void on(PaymentCompletedEvent event) {
        orders.findById(event.orderId()).ifPresent(order -> {
            log.info("✅ [Order] 결제 확인 완료 -> 주문 확정(COMPLETED): {}", order.getId());
            order.complete();
        });
    }

    /**
     * [추가] 결제 실패 시 주문을 취소합니다.
     */
    @ApplicationModuleListener
    public void on(PaymentFailedEvent event) {
        orders.findById(event.orderId()).ifPresent(order -> {
            log.info("📦 [Order] 결제 실패로 인한 주문 취소 처리: {}", order.getId());
            order.cancel();
        });
    }

    /**
     * 재고 부족 시 실행되는 보상 트랜잭션 (기존 로직 유지)
     */
    @Async
    @ApplicationModuleListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void on(InventoryFailedEvent event) {
        orders.findById(event.orderId()).ifPresent(order -> {
            log.info("📦 [Order] 재고 부족으로 인한 주문 취소 처리: {}", order.getId());
            order.cancel();
        });
    }
}

package com.demomodulish.payment;

import com.demomodulish.common.InventoryVerifiedEvent;
import com.demomodulish.common.PaymentCompletedEvent;
import com.demomodulish.common.PaymentFailedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PaymentService {

    private final ApplicationEventPublisher events;
    private final long testFailureAmount;

    public PaymentService(ApplicationEventPublisher events,
                       @Value("${payment.test-failure-amount:9999}") long testFailureAmount) {
        this.events = events;
        this.testFailureAmount = testFailureAmount;
    }

    @ApplicationModuleListener
    public void on(InventoryVerifiedEvent event) {
        if (event.totalAmount() == testFailureAmount) {
            events.publishEvent(new PaymentFailedEvent(
                    event.orderId(),
                    String.format("결제 실패: %d원", event.totalAmount()),
                    event.productId(),
                    event.quantity(),
                    event.deductedBatches()
            ));
            log.info("💸 [Payment] 결제 실패 -> 보상 트랜잭션 발동");
        } else {
            events.publishEvent(new PaymentCompletedEvent(event.orderId()));
            log.info("💰 [Payment] 결제 성공: {}", event.orderId());
        }
    }
}

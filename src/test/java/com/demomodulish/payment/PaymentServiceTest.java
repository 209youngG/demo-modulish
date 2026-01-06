package com.demomodulish.payment;

import com.demomodulish.common.InventoryVerifiedEvent;
import com.demomodulish.common.PaymentCompletedEvent;
import com.demomodulish.common.PaymentFailedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    ApplicationEventPublisher events;

    private PaymentService paymentService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        paymentService = new PaymentService(events, 9999L);
    }

    @Test
    @DisplayName("재고 확인 완료 이벤트를 받으면 결제를 시도하고 성공 시 완료 이벤트를 발행한다")
    void publishesPaymentCompletedEventOnSuccess() {
        InventoryVerifiedEvent event = new InventoryVerifiedEvent("ORDER-123", 2000L, "PRODUCT-123", 2, Map.of());

        paymentService.on(event);

        verify(events).publishEvent(any(PaymentCompletedEvent.class));
    }

    @Test
    @DisplayName("결제 실패 시 실패 이벤트를 발행한다")
    void publishesPaymentFailedEventOnFailure() {
        InventoryVerifiedEvent event = new InventoryVerifiedEvent("FAIL-ORDER", 9999L, "FAIL-PRODUCT", 3, Map.of());

        paymentService.on(event);

        verify(events).publishEvent(any(PaymentFailedEvent.class));
    }
}

package com.demomodulish.order;

import com.demomodulish.common.OrderCompletedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.modulith.test.AssertablePublishedEvents;

@ApplicationModuleTest
class OrderIntegrationTests {
    @Autowired
    OrderManagement orderManagement;

    @Autowired
    OrderRepository orderRepository;

    @Test
    void shouldPersistOrderAndPublishEvent(AssertablePublishedEvents events) {
        // Given
        var order = new Order("PRODUCT-123", 2);

        // When
        orderManagement.place(order);

        // Then 1: DB에 저장이 되었는가?
        var savedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assert savedOrder.getProductId().equals("PRODUCT-123");

        // Then 2: 이벤트가 발행되었는가?
        events.assertThat()
                .contains(OrderCompletedEvent.class)
                .matching(OrderCompletedEvent::orderId, order.getId());
    }

}

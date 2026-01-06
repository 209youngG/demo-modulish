package com.demomodulish.order;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Order 엔티티 단위 테스트")
class OrderTest {

    @Test
    @DisplayName("주문 생성 시 PENDING 상태로 시작한다")
    void shouldStartAsPending() {
        Order order = new Order("PRODUCT-1", 5, 1000);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    @DisplayName("총 금액을 정확히 계산한다")
    void shouldCalculateTotalAmountCorrectly() {
        Order order = new Order("PRODUCT-1", 3, 2000);

        assertThat(order.getTotalAmount()).isEqualTo(6000);
    }

    @Test
    @DisplayName("complete 호출 시 상태를 COMPLETED로 변경한다")
    void shouldChangeStatusToCompletedOnComplete() {
        Order order = new Order("PRODUCT-1", 5, 1000);

        order.complete();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    @DisplayName("cancel 호출 시 상태를 CANCELLED로 변경한다")
    void shouldChangeStatusToCancelledOnCancel() {
        Order order = new Order("PRODUCT-1", 5, 1000);

        order.cancel();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("유효한 주문 정보로 생성한다")
    void shouldCreateValidOrder() {
        Order order = new Order("PRODUCT-1", 5, 1000);

        assertThat(order.getProductId()).isEqualTo("PRODUCT-1");
        assertThat(order.getQuantity()).isEqualTo(5);
        assertThat(order.getPrice()).isEqualTo(1000);
        assertThat(order.getId()).isNotNull();
    }
}

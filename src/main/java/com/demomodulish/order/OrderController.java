package com.demomodulish.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
class OrderController {

    private final OrderManagement orderManagement;

    OrderController(OrderManagement orderManagement) {
        this.orderManagement = orderManagement;
    }

    @PostMapping
    public String placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        Order order = new Order(request.productId(), request.quantity(), request.price());
        orderManagement.place(order);
        return order.getId();
    }

    record PlaceOrderRequest(
            @NotBlank String productId,
            @Min(1) int quantity,
            @Min(0) long price
    ) {}
}

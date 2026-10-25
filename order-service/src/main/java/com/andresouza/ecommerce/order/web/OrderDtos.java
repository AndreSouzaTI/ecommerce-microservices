package com.andresouza.ecommerce.order.web;

import com.andresouza.ecommerce.order.domain.Order;
import com.andresouza.ecommerce.order.domain.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record ItemRequest(@NotNull Long productId, @NotNull @Positive Integer quantity) {
    }

    public record CreateOrderRequest(@NotBlank @Email String customerEmail, @NotEmpty @Valid List<ItemRequest> items) {
    }

    public record ItemResponse(Long productId, String productName, BigDecimal unitPrice, int quantity) {
    }

    public record OrderResponse(Long id, String customerEmail, OrderStatus status, BigDecimal total,
                                String rejectionReason, List<ItemResponse> items, Instant createdAt) {

        public static OrderResponse from(Order order) {
            List<ItemResponse> items = order.getItems().stream()
                    .map(i -> new ItemResponse(i.getProductId(), i.getProductName(), i.getUnitPrice(), i.getQuantity()))
                    .toList();
            return new OrderResponse(order.getId(), order.getCustomerEmail(), order.getStatus(), order.getTotal(),
                    order.getRejectionReason(), items, order.getCreatedAt());
        }
    }
}

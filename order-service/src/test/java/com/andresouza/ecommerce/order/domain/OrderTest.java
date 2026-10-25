package com.andresouza.ecommerce.order.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {

    @Test
    void calculatesTotalFromItems() {
        Order order = new Order("ana@email.com");
        order.addItem(1L, "Notebook", new BigDecimal("5000.00"), 1);
        order.addItem(2L, "Mouse", new BigDecimal("100.00"), 3);

        assertThat(order.getTotal()).isEqualByComparingTo("5300.00");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void sagaRepliesOnlyChangePendingOrders() {
        Order order = new Order("ana@email.com");

        assertThat(order.confirm()).isTrue();
        assertThat(order.reject("atrasado")).isFalse();
        assertThat(order.confirm()).isFalse();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getRejectionReason()).isNull();
    }
}

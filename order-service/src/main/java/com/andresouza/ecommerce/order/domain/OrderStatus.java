package com.andresouza.ecommerce.order.domain;

public enum OrderStatus {
    /** Aguardando a reserva de estoque no catalog-service. */
    PENDING,
    CONFIRMED,
    REJECTED
}

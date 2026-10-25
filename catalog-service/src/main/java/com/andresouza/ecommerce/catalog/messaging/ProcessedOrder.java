package com.andresouza.ecommerce.catalog.messaging;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Pedidos já processados: garante idempotência quando o broker reentrega uma mensagem. */
@Entity
@Table(name = "processed_orders")
public class ProcessedOrder {

    @Id
    @Column(name = "order_id")
    private Long orderId;

    @Column(nullable = false)
    private boolean reserved;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt = Instant.now();

    protected ProcessedOrder() {
    }

    public ProcessedOrder(Long orderId, boolean reserved) {
        this.orderId = orderId;
        this.reserved = reserved;
    }
}

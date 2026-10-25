package com.andresouza.ecommerce.catalog.messaging;

import java.util.List;

/**
 * Contratos das mensagens trocadas com o order-service.
 * Cada serviço mantém a própria cópia: não há biblioteca compartilhada acoplando os deploys.
 */
public final class Events {

    public static final String EXCHANGE = "ecommerce.events";
    public static final String ORDER_CREATED = "order.created";
    public static final String STOCK_RESERVED = "stock.reserved";
    public static final String STOCK_REJECTED = "stock.rejected";

    private Events() {
    }

    public record OrderItem(Long productId, int quantity) {
    }

    public record OrderCreatedEvent(Long orderId, List<OrderItem> items) {
    }

    public record StockReservedEvent(Long orderId) {
    }

    public record StockRejectedEvent(Long orderId, String reason) {
    }
}

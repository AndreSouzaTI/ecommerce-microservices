package com.andresouza.ecommerce.order.messaging;

import com.andresouza.ecommerce.order.messaging.Events.StockRejectedEvent;
import com.andresouza.ecommerce.order.messaging.Events.StockReservedEvent;
import com.andresouza.ecommerce.order.service.OrderService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Fecha a saga: confirma ou rejeita o pedido conforme a resposta do catálogo. */
@Component
public class StockReplyListener {

    private final OrderService orderService;

    public StockReplyListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @RabbitListener(queues = RabbitConfig.STOCK_RESERVED_QUEUE)
    public void onStockReserved(StockReservedEvent event) {
        orderService.confirm(event.orderId());
    }

    @RabbitListener(queues = RabbitConfig.STOCK_REJECTED_QUEUE)
    public void onStockRejected(StockRejectedEvent event) {
        orderService.reject(event.orderId(), event.reason());
    }
}

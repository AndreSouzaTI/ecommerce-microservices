package com.andresouza.ecommerce.catalog.messaging;

import com.andresouza.ecommerce.catalog.messaging.Events.OrderCreatedEvent;
import com.andresouza.ecommerce.catalog.messaging.Events.StockRejectedEvent;
import com.andresouza.ecommerce.catalog.messaging.Events.StockReservedEvent;
import com.andresouza.ecommerce.catalog.messaging.StockReservationService.AlreadyProcessed;
import com.andresouza.ecommerce.catalog.messaging.StockReservationService.Rejected;
import com.andresouza.ecommerce.catalog.messaging.StockReservationService.Reserved;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/** Passo do catálogo na saga: reserva o estoque e responde ao order-service. */
@Component
public class OrderCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedListener.class);

    private final StockReservationService reservationService;
    private final RabbitTemplate rabbit;

    public OrderCreatedListener(StockReservationService reservationService, RabbitTemplate rabbit) {
        this.reservationService = reservationService;
        this.rabbit = rabbit;
    }

    @RabbitListener(queues = RabbitConfig.ORDER_CREATED_QUEUE)
    public void onOrderCreated(OrderCreatedEvent event) {
        switch (reservationService.reserve(event)) {
            case Reserved reserved -> {
                log.info("Estoque reservado para o pedido {}", event.orderId());
                rabbit.convertAndSend(Events.EXCHANGE, Events.STOCK_RESERVED, new StockReservedEvent(event.orderId()));
            }
            case Rejected rejected -> {
                log.info("Pedido {} rejeitado: {}", event.orderId(), rejected.reason());
                rabbit.convertAndSend(Events.EXCHANGE, Events.STOCK_REJECTED,
                        new StockRejectedEvent(event.orderId(), rejected.reason()));
            }
            case AlreadyProcessed alreadyProcessed ->
                    log.info("Pedido {} já processado, mensagem ignorada", event.orderId());
        }
    }
}

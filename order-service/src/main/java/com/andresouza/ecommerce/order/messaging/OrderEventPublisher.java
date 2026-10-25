package com.andresouza.ecommerce.order.messaging;

import com.andresouza.ecommerce.order.messaging.Events.OrderCreatedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publica no RabbitMQ somente depois do commit: se a transação do pedido
 * falhar, nenhum evento "fantasma" chega ao catálogo.
 */
@Component
public class OrderEventPublisher {

    private final RabbitTemplate rabbit;

    public OrderEventPublisher(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(OrderCreatedEvent event) {
        rabbit.convertAndSend(Events.EXCHANGE, Events.ORDER_CREATED, event);
    }
}

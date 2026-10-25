package com.andresouza.ecommerce.catalog;

import com.andresouza.ecommerce.catalog.messaging.Events;
import com.andresouza.ecommerce.catalog.messaging.Events.OrderCreatedEvent;
import com.andresouza.ecommerce.catalog.messaging.Events.OrderItem;
import com.andresouza.ecommerce.catalog.product.Product;
import com.andresouza.ecommerce.catalog.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sobe o serviço com PostgreSQL e RabbitMQ reais (Testcontainers) e verifica
 * o passo do catálogo na saga: consumir OrderCreated e responder com a reserva.
 */
@SpringBootTest(properties = "eureka.client.enabled=false")
@Import(TestcontainersConfiguration.class)
class StockReservationIntegrationTest {

    private static final String REPLIES = "test.stock-replies";

    @Autowired
    private RabbitTemplate rabbit;
    @Autowired
    private AmqpAdmin admin;
    @Autowired
    private ProductRepository products;

    @BeforeEach
    void listenToReplies() {
        Queue queue = new Queue(REPLIES, true, false, false);
        admin.declareQueue(queue);
        admin.declareBinding(BindingBuilder.bind(queue).to(new TopicExchange(Events.EXCHANGE)).with("stock.*"));
        admin.purgeQueue(REPLIES, false);
    }

    @Test
    void reservesStockAndRepliesWithStockReserved() {
        Product product = products.save(new Product("Webcam Full HD", new BigDecimal("199.90"), 5));

        rabbit.convertAndSend(Events.EXCHANGE, Events.ORDER_CREATED,
                new OrderCreatedEvent(1001L, List.of(new OrderItem(product.getId(), 3))));

        Message reply = rabbit.receive(REPLIES, 10_000);
        assertThat(reply).isNotNull();
        assertThat(reply.getMessageProperties().getReceivedRoutingKey()).isEqualTo(Events.STOCK_RESERVED);
        assertThat(products.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(2);
    }

    @Test
    void rejectsWhenStockIsInsufficientAndKeepsStockUntouched() {
        Product product = products.save(new Product("Microfone", new BigDecimal("350.00"), 1));

        rabbit.convertAndSend(Events.EXCHANGE, Events.ORDER_CREATED,
                new OrderCreatedEvent(1002L, List.of(new OrderItem(product.getId(), 2))));

        Message reply = rabbit.receive(REPLIES, 10_000);
        assertThat(reply).isNotNull();
        assertThat(reply.getMessageProperties().getReceivedRoutingKey()).isEqualTo(Events.STOCK_REJECTED);
        assertThat(new String(reply.getBody(), StandardCharsets.UTF_8)).contains("Estoque insuficiente");
        assertThat(products.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(1);
    }

    @Test
    void ignoresDuplicatedMessages() {
        Product product = products.save(new Product("Hub USB", new BigDecimal("89.90"), 10));
        OrderCreatedEvent event = new OrderCreatedEvent(1003L, List.of(new OrderItem(product.getId(), 4)));

        rabbit.convertAndSend(Events.EXCHANGE, Events.ORDER_CREATED, event);
        assertThat(rabbit.receive(REPLIES, 10_000)).isNotNull();

        rabbit.convertAndSend(Events.EXCHANGE, Events.ORDER_CREATED, event);
        assertThat(rabbit.receive(REPLIES, 3_000)).isNull();
        assertThat(products.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(6);
    }
}

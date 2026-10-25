package com.andresouza.ecommerce.order;

import com.andresouza.ecommerce.order.catalog.CatalogClient;
import com.andresouza.ecommerce.order.catalog.CatalogClient.ProductView;
import com.andresouza.ecommerce.order.catalog.CatalogUnavailableException;
import com.andresouza.ecommerce.order.messaging.Events;
import com.andresouza.ecommerce.order.messaging.Events.StockRejectedEvent;
import com.andresouza.ecommerce.order.messaging.Events.StockReservedEvent;
import com.jayway.jsonpath.JsonPath;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercita a saga do lado do order-service com PostgreSQL e RabbitMQ reais.
 * O catalog-service é simulado: o teste publica as respostas que ele enviaria.
 */
@SpringBootTest(properties = "eureka.client.enabled=false")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OrderSagaIntegrationTest {

    private static final String CREATED_EVENTS = "test.order-created";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private RabbitTemplate rabbit;
    @Autowired
    private AmqpAdmin admin;
    @MockitoBean
    private CatalogClient catalog;

    @BeforeEach
    void setUp() {
        Queue queue = new Queue(CREATED_EVENTS, true, false, false);
        admin.declareQueue(queue);
        admin.declareBinding(BindingBuilder.bind(queue).to(new TopicExchange(Events.EXCHANGE)).with(Events.ORDER_CREATED));
        admin.purgeQueue(CREATED_EVENTS, false);
        when(catalog.findProduct(anyLong())).thenAnswer(invocation -> Optional.of(
                new ProductView(invocation.getArgument(0), "Monitor 27", new BigDecimal("2000.00"), 10)));
    }

    @Test
    void orderIsConfirmedWhenStockIsReserved() throws Exception {
        long orderId = createOrder();

        Message event = rabbit.receive(CREATED_EVENTS, 10_000);
        assertThat(event).isNotNull();
        assertThat(new String(event.getBody(), StandardCharsets.UTF_8)).contains("\"orderId\":" + orderId);

        rabbit.convertAndSend(Events.EXCHANGE, Events.STOCK_RESERVED, new StockReservedEvent(orderId));

        assertThat(waitForStatus(orderId, "CONFIRMED")).isTrue();
    }

    @Test
    void orderIsRejectedWhenStockIsNotAvailable() throws Exception {
        long orderId = createOrder();
        assertThat(rabbit.receive(CREATED_EVENTS, 10_000)).isNotNull();

        rabbit.convertAndSend(Events.EXCHANGE, Events.STOCK_REJECTED, new StockRejectedEvent(orderId, "Estoque insuficiente"));

        assertThat(waitForStatus(orderId, "REJECTED")).isTrue();
        mvc.perform(get("/api/orders/{id}", orderId))
                .andExpect(jsonPath("$.rejectionReason").value("Estoque insuficiente"));
    }

    @Test
    void returns503WhenCatalogIsUnavailable() throws Exception {
        when(catalog.findProduct(anyLong())).thenThrow(new CatalogUnavailableException(new RuntimeException("timeout")));

        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerEmail\": \"ana@email.com\", \"items\": [{\"productId\": 1, \"quantity\": 1}]}"))
                .andExpect(status().isServiceUnavailable());
        assertThat(rabbit.receive(CREATED_EVENTS, 2_000)).isNull();
    }

    private long createOrder() throws Exception {
        String body = mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerEmail\": \"ana@email.com\", \"items\": [{\"productId\": 2, \"quantity\": 2}]}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.total").value(4000.00))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private boolean waitForStatus(long orderId, String expected) throws Exception {
        for (int attempt = 0; attempt < 50; attempt++) {
            String body = mvc.perform(get("/api/orders/{id}", orderId)).andReturn().getResponse().getContentAsString();
            if (expected.equals(JsonPath.read(body, "$.status"))) {
                return true;
            }
            Thread.sleep(200);
        }
        return false;
    }
}

package com.andresouza.ecommerce.catalog.messaging;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String ORDER_CREATED_QUEUE = "catalog.order-created";

    @Bean
    TopicExchange eventsExchange() {
        return new TopicExchange(Events.EXCHANGE);
    }

    @Bean
    Queue orderCreatedQueue() {
        // Mensagens que falharem após as tentativas vão para a DLQ em vez de ficar em loop
        return QueueBuilder.durable(ORDER_CREATED_QUEUE)
                .deadLetterExchange("")
                .deadLetterRoutingKey(ORDER_CREATED_QUEUE + ".dlq")
                .build();
    }

    @Bean
    Queue orderCreatedDeadLetterQueue() {
        return QueueBuilder.durable(ORDER_CREATED_QUEUE + ".dlq").build();
    }

    @Bean
    Binding orderCreatedBinding(Queue orderCreatedQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(orderCreatedQueue).to(eventsExchange).with(Events.ORDER_CREATED);
    }

    @Bean
    MessageConverter messageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        // Converte pelo tipo do parâmetro do listener, e não pelo nome da classe de quem publicou
        converter.setAlwaysConvertToInferredType(true);
        return converter;
    }
}

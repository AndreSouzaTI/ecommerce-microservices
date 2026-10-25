package com.andresouza.ecommerce.order.messaging;

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

    public static final String STOCK_RESERVED_QUEUE = "order.stock-reserved";
    public static final String STOCK_REJECTED_QUEUE = "order.stock-rejected";

    @Bean
    TopicExchange eventsExchange() {
        return new TopicExchange(Events.EXCHANGE);
    }

    @Bean
    Queue stockReservedQueue() {
        return QueueBuilder.durable(STOCK_RESERVED_QUEUE).build();
    }

    @Bean
    Queue stockRejectedQueue() {
        return QueueBuilder.durable(STOCK_REJECTED_QUEUE).build();
    }

    @Bean
    Binding stockReservedBinding(Queue stockReservedQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(stockReservedQueue).to(eventsExchange).with(Events.STOCK_RESERVED);
    }

    @Bean
    Binding stockRejectedBinding(Queue stockRejectedQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(stockRejectedQueue).to(eventsExchange).with(Events.STOCK_REJECTED);
    }

    @Bean
    MessageConverter messageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        converter.setAlwaysConvertToInferredType(true);
        return converter;
    }
}

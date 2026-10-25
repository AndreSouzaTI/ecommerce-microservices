package com.andresouza.ecommerce.order.service;

import com.andresouza.ecommerce.order.catalog.CatalogClient;
import com.andresouza.ecommerce.order.catalog.CatalogClient.ProductView;
import com.andresouza.ecommerce.order.domain.Order;
import com.andresouza.ecommerce.order.domain.OrderRepository;
import com.andresouza.ecommerce.order.messaging.Events.OrderCreatedEvent;
import com.andresouza.ecommerce.order.messaging.Events.OrderItem;
import com.andresouza.ecommerce.order.web.OrderDtos.CreateOrderRequest;
import com.andresouza.ecommerce.order.web.OrderDtos.OrderResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orders;
    private final CatalogClient catalog;
    private final ApplicationEventPublisher events;

    public OrderService(OrderRepository orders, CatalogClient catalog, ApplicationEventPublisher events) {
        this.orders = orders;
        this.catalog = catalog;
        this.events = events;
    }

    /**
     * Grava o pedido como PENDING com o preço atual do catálogo e dispara a saga.
     * A reserva de estoque acontece de forma assíncrona no catalog-service.
     */
    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        Order order = new Order(request.customerEmail());
        request.items().forEach(item -> {
            ProductView product = catalog.findProduct(item.productId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                            "Produto " + item.productId() + " não existe"));
            order.addItem(product.id(), product.name(), product.price(), item.quantity());
        });
        orders.save(order);

        List<OrderItem> items = order.getItems().stream()
                .map(item -> new OrderItem(item.getProductId(), item.getQuantity()))
                .toList();
        events.publishEvent(new OrderCreatedEvent(order.getId(), items));
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        return orders.findWithItemsById(id)
                .map(OrderResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido " + id + " não encontrado"));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findByCustomer(String email) {
        return orders.findByCustomerEmailOrderByCreatedAtDesc(email).stream().map(OrderResponse::from).toList();
    }

    @Transactional
    public void confirm(Long orderId) {
        orders.findById(orderId).ifPresentOrElse(
                order -> {
                    if (order.confirm()) {
                        log.info("Pedido {} confirmado", orderId);
                    }
                },
                () -> log.warn("Confirmação recebida para pedido inexistente {}", orderId));
    }

    @Transactional
    public void reject(Long orderId, String reason) {
        orders.findById(orderId).ifPresentOrElse(
                order -> {
                    if (order.reject(reason)) {
                        log.info("Pedido {} rejeitado: {}", orderId, reason);
                    }
                },
                () -> log.warn("Rejeição recebida para pedido inexistente {}", orderId));
    }
}

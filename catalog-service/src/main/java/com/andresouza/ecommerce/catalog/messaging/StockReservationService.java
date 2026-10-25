package com.andresouza.ecommerce.catalog.messaging;

import com.andresouza.ecommerce.catalog.messaging.Events.OrderCreatedEvent;
import com.andresouza.ecommerce.catalog.messaging.Events.OrderItem;
import com.andresouza.ecommerce.catalog.product.Product;
import com.andresouza.ecommerce.catalog.product.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StockReservationService {

    public sealed interface Result permits Reserved, Rejected, AlreadyProcessed {
    }

    public record Reserved() implements Result {
    }

    public record Rejected(String reason) implements Result {
    }

    public record AlreadyProcessed() implements Result {
    }

    private final ProductRepository products;
    private final ProcessedOrderRepository processedOrders;

    public StockReservationService(ProductRepository products, ProcessedOrderRepository processedOrders) {
        this.products = products;
        this.processedOrders = processedOrders;
    }

    /** Reserva todos os itens ou nenhum, na mesma transação. */
    @Transactional
    public Result reserve(OrderCreatedEvent event) {
        if (processedOrders.existsById(event.orderId())) {
            return new AlreadyProcessed();
        }
        Map<Long, Product> found = products.findAllById(event.items().stream().map(OrderItem::productId).toList())
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        Optional<String> problem = event.items().stream()
                .map(item -> validate(item, found.get(item.productId())))
                .flatMap(Optional::stream)
                .findFirst();

        if (problem.isPresent()) {
            processedOrders.save(new ProcessedOrder(event.orderId(), false));
            return new Rejected(problem.get());
        }
        event.items().forEach(item -> found.get(item.productId()).reserve(item.quantity()));
        processedOrders.save(new ProcessedOrder(event.orderId(), true));
        return new Reserved();
    }

    private Optional<String> validate(OrderItem item, Product product) {
        if (product == null) {
            return Optional.of("Produto " + item.productId() + " não existe");
        }
        if (!product.hasStock(item.quantity())) {
            return Optional.of("Estoque insuficiente para " + product.getName());
        }
        return Optional.empty();
    }
}

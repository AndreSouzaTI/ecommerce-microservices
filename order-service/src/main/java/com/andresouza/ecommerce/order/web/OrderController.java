package com.andresouza.ecommerce.order.web;

import com.andresouza.ecommerce.order.catalog.CatalogUnavailableException;
import com.andresouza.ecommerce.order.service.OrderService;
import com.andresouza.ecommerce.order.web.OrderDtos.CreateOrderRequest;
import com.andresouza.ecommerce.order.web.OrderDtos.OrderResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** Retorna 202: o pedido foi aceito, a confirmação chega de forma assíncrona. */
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.create(request);
    }

    @GetMapping("/{id}")
    public OrderResponse findById(@PathVariable Long id) {
        return orderService.findById(id);
    }

    @GetMapping
    public List<OrderResponse> findByCustomer(@RequestParam String customerEmail) {
        return orderService.findByCustomer(customerEmail);
    }

    @ExceptionHandler(CatalogUnavailableException.class)
    ProblemDetail handleCatalogUnavailable(CatalogUnavailableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }
}

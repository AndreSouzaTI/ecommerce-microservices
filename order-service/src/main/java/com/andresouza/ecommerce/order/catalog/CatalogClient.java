package com.andresouza.ecommerce.order.catalog;

import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Consulta síncrona ao catalog-service, resolvido pelo Eureka ("http://catalog-service")
 * e protegida por circuit breaker (Resilience4j).
 */
@Component
public class CatalogClient {

    public record ProductView(Long id, String name, BigDecimal price, int stockQuantity) {
    }

    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;

    public CatalogClient(@LoadBalanced RestClient.Builder loadBalancedBuilder, CircuitBreakerFactory<?, ?> circuitBreakerFactory) {
        this.restClient = loadBalancedBuilder.baseUrl("http://catalog-service").build();
        this.circuitBreaker = circuitBreakerFactory.create("catalog");
    }

    public Optional<ProductView> findProduct(Long id) {
        return circuitBreaker.run(() -> fetch(id), error -> {
            throw new CatalogUnavailableException(error);
        });
    }

    private Optional<ProductView> fetch(Long id) {
        try {
            return Optional.ofNullable(restClient.get().uri("/api/products/{id}", id).retrieve().body(ProductView.class));
        } catch (HttpClientErrorException.NotFound notFound) {
            // 404 é uma resposta válida do catálogo, não deve abrir o circuito
            return Optional.empty();
        }
    }
}

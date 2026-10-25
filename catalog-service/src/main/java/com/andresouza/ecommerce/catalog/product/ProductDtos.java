package com.andresouza.ecommerce.catalog.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public final class ProductDtos {

    private ProductDtos() {
    }

    public record ProductRequest(@NotBlank String name,
                                 @NotNull @DecimalMin("0.01") BigDecimal price,
                                 @NotNull @PositiveOrZero Integer stockQuantity) {
    }

    public record ProductResponse(Long id, String name, BigDecimal price, int stockQuantity) {

        static ProductResponse from(Product product) {
            return new ProductResponse(product.getId(), product.getName(), product.getPrice(), product.getStockQuantity());
        }
    }
}

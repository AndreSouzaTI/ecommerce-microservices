package com.andresouza.ecommerce.order.catalog;

public class CatalogUnavailableException extends RuntimeException {
    public CatalogUnavailableException(Throwable cause) {
        super("Catálogo indisponível no momento. Tente novamente em instantes.", cause);
    }
}

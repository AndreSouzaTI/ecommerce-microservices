package com.andresouza.ecommerce.catalog.product;

import com.andresouza.ecommerce.catalog.product.ProductDtos.ProductRequest;
import com.andresouza.ecommerce.catalog.product.ProductDtos.ProductResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository products;

    public ProductController(ProductRepository products) {
        this.products = products;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<ProductResponse> list() {
        return products.findAll().stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ProductResponse findById(@PathVariable Long id) {
        return products.findById(id)
                .map(ProductResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto " + id + " não encontrado"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return ProductResponse.from(products.save(new Product(request.name(), request.price(), request.stockQuantity())));
    }
}

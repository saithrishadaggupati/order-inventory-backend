package com.orderinventory.orderinventory.service;

import com.orderinventory.orderinventory.entity.Product;
import com.orderinventory.orderinventory.repository.ProductRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // this lives in its own class (not the controller) so spring's proxy actually
    // intercepts the call - calling this method from within the same class as the
    // caller would bypass caching entirely, same issue we hit earlier with @Transactional
    @Cacheable(value = "products", key = "#id")
    public Product getCachedProduct(Long id) {
        System.out.println("Fetching product " + id + " from DATABASE (not cache)");
        return productRepository.findById(id).orElse(null);
    }
}
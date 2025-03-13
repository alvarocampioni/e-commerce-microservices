package com.ms.cart_service.service;

import com.ms.cart_service.exception.ResourceNotFoundException;
import com.ms.cart_service.model.Product;
import com.ms.cart_service.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    @Autowired
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public void addProduct(Product product) {
        productRepository.save(product);
    }

    public void updateProduct(Product product) {
        productRepository.save(product);
    }

    public void deleteProduct(Product product) {
        productRepository.delete(product);
    }

    public boolean existsProduct(String productId, int amount) {
        return productRepository.existsByProductIdAndAmountIsGreaterThanEqual(productId, amount);
    }

    public String getName(String productId) {
        Optional<Product> product = productRepository.findById(productId);
        if (product.isPresent()) {
            return product.get().getProductName();
        }

        throw new ResourceNotFoundException("Product not found with ID: " + productId);
    }
}

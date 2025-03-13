package com.ms.cart_service.repository;

import com.ms.cart_service.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, String> {
    boolean existsByProductIdAndAmountIsGreaterThanEqual(String productId, int amount);
}

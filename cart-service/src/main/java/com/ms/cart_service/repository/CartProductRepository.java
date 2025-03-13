package com.ms.cart_service.repository;

import com.ms.cart_service.model.CartProduct;
import com.ms.cart_service.model.CartProductId;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartProductRepository extends JpaRepository<CartProduct, CartProductId> {
    List<CartProduct> findByCustomerId(String customerId);
    CartProduct findByCustomerIdAndProductId(String customerId, String productId);
    void deleteByCustomerId(String customerId);
    void deleteByCustomerIdAndProductId(String customerId, String productId);

    @CacheEvict(value = "*", allEntries = true)
    void deleteAll();
}

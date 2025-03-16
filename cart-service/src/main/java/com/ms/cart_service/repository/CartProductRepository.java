package com.ms.cart_service.repository;

import com.ms.cart_service.model.CartProduct;
import com.ms.cart_service.model.CartProductId;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartProductRepository extends JpaRepository<CartProduct, CartProductId> {
    List<CartProduct> findByEmail(String email);
    CartProduct findByEmailAndProductId(String email, String productId);
    void deleteByEmail(String email);
    void deleteByEmailAndProductId(String email, String productId);

    @CacheEvict(value = "*", allEntries = true)
    void deleteAll();
}

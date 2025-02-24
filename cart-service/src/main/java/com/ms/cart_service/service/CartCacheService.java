package com.ms.cart_service.service;

import com.ms.cart_service.dto.CartDTO;
import com.ms.cart_service.model.CartProduct;
import com.ms.cart_service.repository.CartProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class CartCacheService {

    private final CartProductRepository cartProductRepository;

    @Autowired
    public CartCacheService(CartProductRepository cartProductRepository) {
        this.cartProductRepository = cartProductRepository;
    }

    @Cacheable(value = "cart", key = "#customerId")
    public CartDTO getCartByCustomerId(String customerId) {
        log.info("getCartByCustomerId called -- accessing database");
        List<CartProduct> fetchedCart = cartProductRepository.findByCustomerId(customerId);
        return new CartDTO(fetchedCart);
    }
}

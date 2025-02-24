package com.ms.cart_service.service;

import com.ms.cart_service.client.ProductClient;
import com.ms.cart_service.dto.CartDTO;
import com.ms.cart_service.dto.CartProductRequest;
import com.ms.cart_service.events.CartEventProducer;
import com.ms.cart_service.exception.ProductNotAvailableException;
import com.ms.cart_service.exception.ResourceNotFoundException;
import com.ms.cart_service.exception.ServiceUnavailableException;
import com.ms.cart_service.model.CartProduct;
import com.ms.cart_service.repository.CartProductRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;


@Service
public class CartProductService {

    private final CartProductRepository cartProductRepository;
    private final CartCacheService cartCacheService;
    private final ProductClient productClient;
    private final CartEventProducer cartEventProducer;

    @Autowired
    public CartProductService(CartProductRepository cartProductRepository, CartCacheService cartCacheService, ProductClient productClient, CartEventProducer cartEventProducer) {
        this.cartProductRepository = cartProductRepository;
        this.cartCacheService = cartCacheService;
        this.productClient = productClient;
        this.cartEventProducer = cartEventProducer;
    }

    @Transactional
    @CircuitBreaker(name = "cart", fallbackMethod = "fallback")
    @CacheEvict(value = "cart", key = "#customerId")
    public void addToCart(CartProductRequest cartProductRequest, String customerId) {
        CartProduct product = cartProductRepository.findByCustomerIdAndProductId(customerId, cartProductRequest.productId());
        int total = cartProductRequest.amount();
        if(product != null) {
            total += product.getAmount();
        }

        if(productClient.isAvailable(cartProductRequest.productId(), total)) {
            String name = productClient.getName(cartProductRequest.productId());
            CartProduct cartProduct = new CartProduct(customerId, cartProductRequest.productId(), name, total);
            cartProductRepository.save(cartProduct);
        } else {
            throw new ProductNotAvailableException("Product with ID: " + cartProductRequest.productId() + " is not available with the amount: " + total);
        }
    }

    public void fallback(CartProductRequest cartProductRequest, String email, Throwable throwable) {
        throw new ServiceUnavailableException("Failed to fetch product with ID: " + cartProductRequest.productId());
    }

    @Transactional
    @CacheEvict(value = "cart", key = "#customerId")
    public void deleteCart(String customerId) {
        cartProductRepository.deleteByCustomerId(customerId);
    }

    @Transactional
    @CacheEvict(value = "cart", key = "#customerId")
    public void deleteProduct(String customerId, String productId) {
        CartProduct product = cartProductRepository.findByCustomerIdAndProductId(customerId, productId);
        if (product == null) {
            throw new ResourceNotFoundException("Product with ID: " + productId + " not found on cart");
        }
        cartProductRepository.deleteByCustomerIdAndProductId(customerId, productId);
    }

    public void placeOrder(String customerId){
        CartDTO cartDTO = cartCacheService.getCartByCustomerId(customerId);
        if(cartDTO.cart() != null && !cartDTO.cart().isEmpty()) {
            cartEventProducer.sendOrder(cartDTO);
        }
    }
    
}

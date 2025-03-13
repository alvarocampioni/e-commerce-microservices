package com.ms.cart_service.service;

import com.ms.cart_service.dto.CartDTO;
import com.ms.cart_service.dto.CartProductRequest;
import com.ms.cart_service.events.CartEventProducer;
import com.ms.cart_service.exception.ProductNotAvailableException;
import com.ms.cart_service.exception.ResourceNotFoundException;
import com.ms.cart_service.model.CartProduct;
import com.ms.cart_service.repository.CartProductRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;


@Service
public class CartProductService {

    private final CartProductRepository cartProductRepository;
    private final CartCacheService cartCacheService;
    private final ProductService productService;
    private final CartEventProducer cartEventProducer;

    @Autowired
    public CartProductService(CartProductRepository cartProductRepository, CartCacheService cartCacheService, ProductService productService, CartEventProducer cartEventProducer) {
        this.cartProductRepository = cartProductRepository;
        this.cartCacheService = cartCacheService;
        this.productService = productService;
        this.cartEventProducer = cartEventProducer;
    }

    @Transactional
    @CacheEvict(value = "cart", key = "#customerId")
    public void addToCart(CartProductRequest cartProductRequest, String customerId) {
        CartProduct product = cartProductRepository.findByCustomerIdAndProductId(customerId, cartProductRequest.productId());
        int total = cartProductRequest.amount();
        if(product != null) {
            total += product.getAmount();
        }

        if(isAvailable(cartProductRequest.productId(), total)) {
            String name = productService.getName(cartProductRequest.productId());
            CartProduct cartProduct = new CartProduct(customerId, cartProductRequest.productId(), name, total);
            cartProductRepository.save(cartProduct);
        } else {
            throw new ProductNotAvailableException("Product with ID: " + cartProductRequest.productId() + " is not available with the amount: " + total);
        }
    }

    private boolean isAvailable(String productId, int amount){
        return productService.existsProduct(productId, amount);
    }

    @Transactional
    @CacheEvict(value = "cart", key = "#customerId")
    public void deleteCart(String customerId) {
        cartProductRepository.deleteByCustomerId(customerId);
    }

    @Transactional
    @CacheEvict(value = "cart", key = "#customerId")
    public void removeProduct(String customerId, String productId) {
        CartProduct product = cartProductRepository.findByCustomerIdAndProductId(customerId, productId);
        if (product == null) {
            throw new ResourceNotFoundException("Product with ID: " + productId + " not found on cart");
        }
        cartProductRepository.deleteByCustomerIdAndProductId(customerId, productId);
    }

    @Transactional
    @CacheEvict(value = "cart", key = "#customerId")
    public void placeOrder(String customerId) {
        CartDTO cartDTO = cartCacheService.getCartByCustomerId(customerId);
        if (cartDTO.cart() != null && !cartDTO.cart().isEmpty()) {
            cartEventProducer.sendOrder(cartDTO);
            deleteCart(customerId);
        }
    }
}

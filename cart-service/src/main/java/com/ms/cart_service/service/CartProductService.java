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
    @CacheEvict(value = "cart", key = "#email")
    public void addToCart(CartProductRequest cartProductRequest, String email) {
        if(cartProductRequest.amount() <= 0){
            throw new ProductNotAvailableException("Amount must be greater than zero.");
        }
        CartProduct product = cartProductRepository.findByEmailAndProductId(email, cartProductRequest.productId());
        int total = cartProductRequest.amount();
        if(product != null) {
            total += product.getAmount();
        }

        if(isAvailable(cartProductRequest.productId(), total)) {
            String name = productService.getName(cartProductRequest.productId());
            CartProduct cartProduct = new CartProduct(email, cartProductRequest.productId(), name, total);
            cartProductRepository.save(cartProduct);
        } else {
            throw new ProductNotAvailableException("Product with ID: " + cartProductRequest.productId() + " is not available with the amount: " + total);
        }
    }

    @Transactional
    @CacheEvict(value = "cart", key = "#email")
    public void updateCart(CartProductRequest cartProductRequest, String email) {
        CartProduct product = cartProductRepository.findByEmailAndProductId(email, cartProductRequest.productId());
        if (product == null) {
            throw new ResourceNotFoundException("Product with ID: " + cartProductRequest.productId() + " not found in cart.");
        }

        int requestedAmount = cartProductRequest.amount();
        String productId = cartProductRequest.productId();
        if (!isAvailable(productId, requestedAmount)) {
            throw new ProductNotAvailableException("Product with ID: " + productId + " is not available with the amount: " + requestedAmount);
        }

        if (requestedAmount == 0) {
            cartProductRepository.deleteByEmailAndProductId(email, productId);
        } else {
            product.setAmount(requestedAmount);
            cartProductRepository.save(product);
        }
    }

    private boolean isAvailable(String productId, int amount){
        return productService.existsProduct(productId, amount);
    }

    @Transactional
    @CacheEvict(value = "cart", key = "#email")
    public void deleteCart(String email) {
        cartProductRepository.deleteByEmail(email);
    }

    @Transactional
    @CacheEvict(value = "cart", key = "#email")
    public void removeProduct(String email, String productId) {
        CartProduct product = cartProductRepository.findByEmailAndProductId(email, productId);
        if (product == null) {
            throw new ResourceNotFoundException("Product with ID: " + productId + " not found on cart");
        }
        cartProductRepository.deleteByEmailAndProductId(email, productId);
    }

    @Transactional
    @CacheEvict(value = "cart", key = "#email")
    public void placeOrder(String email) {
        CartDTO cartDTO = cartCacheService.getCartByEmail(email);
        if (cartDTO.cart() != null && !cartDTO.cart().isEmpty()) {
            cartEventProducer.sendOrder(cartDTO);
            deleteCart(email);
        }
    }
}

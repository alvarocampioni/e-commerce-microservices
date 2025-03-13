package com.ms.cart_service.controller;

import com.ms.cart_service.dto.CartDTO;
import com.ms.cart_service.dto.CartProductRequest;
import com.ms.cart_service.service.CartCacheService;
import com.ms.cart_service.service.CartProductService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartProductController {

    private final CartProductService cartProductService;
    private final CartCacheService cartCacheService;

    @Autowired
    public CartProductController(CartProductService cartProductService, CartCacheService cartCacheService) {
        this.cartProductService = cartProductService;
        this.cartCacheService = cartCacheService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<String> addToCart(@RequestBody CartProductRequest cartProductRequest, @RequestHeader(value = "X-USER-EMAIL") String customerId) {
        cartProductService.addToCart(cartProductRequest, customerId);
        return new ResponseEntity<>("Product added successfully !", HttpStatus.CREATED);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<CartDTO> getCartByCustomerId(@RequestHeader(value = "X-USER-EMAIL") String customerId){
        return new ResponseEntity<>(cartCacheService.getCartByCustomerId(customerId), HttpStatus.OK);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> deleteCartByCustomerId(@RequestHeader(value = "X-USER-EMAIL") String customerId){
        cartProductService.deleteCart(customerId);
        return new ResponseEntity<>("Cart cleared successfully !", HttpStatus.OK);
    }

    @DeleteMapping("/product/{productId}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> deleteProduct(@RequestHeader(value = "X-USER-EMAIL") String customerId, @PathVariable String productId){
        cartProductService.removeProduct(customerId, productId);
        return new ResponseEntity<>("Product removed successfully !", HttpStatus.OK);
    }
}

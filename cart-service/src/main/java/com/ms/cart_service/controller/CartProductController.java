package com.ms.cart_service.controller;

import com.ms.cart_service.dto.CartDTO;
import com.ms.cart_service.dto.CartProductRequest;
import com.ms.cart_service.service.CartCacheService;
import com.ms.cart_service.service.CartProductService;
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
    public ResponseEntity<String> addToCart(@RequestBody CartProductRequest cartProductRequest, @RequestHeader(value = "X-USER-EMAIL") String email) {
        cartProductService.addToCart(cartProductRequest, email);
        return new ResponseEntity<>("Product added successfully !", HttpStatus.CREATED);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<CartDTO> getCartByEmail(@RequestHeader(value = "X-USER-EMAIL") String email){
        return new ResponseEntity<>(cartCacheService.getCartByEmail(email), HttpStatus.OK);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> deleteCartByEmail(@RequestHeader(value = "X-USER-EMAIL") String email){
        cartProductService.deleteCart(email);
        return new ResponseEntity<>("Cart cleared successfully !", HttpStatus.OK);
    }

    @DeleteMapping("/product/{productId}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> deleteProduct(@RequestHeader(value = "X-USER-EMAIL") String email, @PathVariable String productId){
        cartProductService.removeProduct(email, productId);
        return new ResponseEntity<>("Product removed successfully !", HttpStatus.OK);
    }
}

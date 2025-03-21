package com.ms.cart_service.controller;

import com.ms.cart_service.dto.CartDTO;
import com.ms.cart_service.dto.CartProductRequest;
import com.ms.cart_service.service.CartCacheService;
import com.ms.cart_service.service.CartProductService;
import jakarta.servlet.http.HttpServletRequest;
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
    public ResponseEntity<String> addToCart(@RequestBody CartProductRequest cartProductRequest, HttpServletRequest request) {
        String email = request.getHeader("X-USER-EMAIL");
        cartProductService.addToCart(cartProductRequest, email);
        return new ResponseEntity<>("Product added successfully !", HttpStatus.CREATED);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<CartDTO> getCartByEmail(HttpServletRequest request) {
        String email = request.getHeader("X-USER-EMAIL");
        return new ResponseEntity<>(cartCacheService.getCartByEmail(email), HttpStatus.OK);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> deleteCartByEmail(HttpServletRequest request) {
        String email = request.getHeader("X-USER-EMAIL");
        cartProductService.deleteCart(email);
        return new ResponseEntity<>("Cart cleared successfully !", HttpStatus.OK);
    }

    @DeleteMapping("/product/{productId}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> deleteProduct(@PathVariable String productId, HttpServletRequest request) {
        String email = request.getHeader("X-USER-EMAIL");
        cartProductService.removeProduct(email, productId);
        return new ResponseEntity<>("Product removed successfully !", HttpStatus.OK);
    }
}

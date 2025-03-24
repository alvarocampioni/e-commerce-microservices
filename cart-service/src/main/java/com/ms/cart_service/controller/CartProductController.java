package com.ms.cart_service.controller;

import com.ms.cart_service.dto.CartDTO;
import com.ms.cart_service.dto.CartProductRequest;
import com.ms.cart_service.service.CartCacheService;
import com.ms.cart_service.service.CartProductService;
import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(summary = "Add Product", description = "Add product to the user cart.")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<String> addToCart(@RequestBody CartProductRequest cartProductRequest, HttpServletRequest request) {
        String email = request.getHeader("X-USER-EMAIL");
        cartProductService.addToCart(cartProductRequest, email);
        return new ResponseEntity<>("Product added successfully !", HttpStatus.CREATED);
    }

    @PutMapping
    @Operation(summary = "Update Product", description = "Update product in the user cart.")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> updateCart(@RequestBody CartProductRequest cartProductRequest, HttpServletRequest request) {
        String email = request.getHeader("X-USER-EMAIL");
        cartProductService.updateCart(cartProductRequest, email);
        return new ResponseEntity<>("Product updated successfully !", HttpStatus.OK);
    }

    @GetMapping
    @Operation(summary = "User Cart", description = "Returns the user cart with the products information.")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<CartDTO> getCartByEmail(HttpServletRequest request) {
        String email = request.getHeader("X-USER-EMAIL");
        return new ResponseEntity<>(cartCacheService.getCartByEmail(email), HttpStatus.OK);
    }

    @DeleteMapping
    @Operation(summary = "Empty Cart", description = "Deletes all products inside the user cart")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> deleteCartByEmail(HttpServletRequest request) {
        String email = request.getHeader("X-USER-EMAIL");
        cartProductService.deleteCart(email);
        return new ResponseEntity<>("Cart cleared successfully !", HttpStatus.OK);
    }

    @DeleteMapping("/product/{productId}")
    @Operation(summary = "Delete Product", description = "Removes a product from the user cart specified by its ID.")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> deleteProduct(@PathVariable String productId, HttpServletRequest request) {
        String email = request.getHeader("X-USER-EMAIL");
        cartProductService.removeProduct(email, productId);
        return new ResponseEntity<>("Product removed successfully !", HttpStatus.OK);
    }
}

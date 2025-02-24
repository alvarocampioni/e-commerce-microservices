package com.ms.product_service.controller;

import com.ms.product_service.dto.OrderDTO;
import com.ms.product_service.dto.ProductCategory;
import com.ms.product_service.dto.ProductRequest;
import com.ms.product_service.dto.ProductResponse;
import com.ms.product_service.model.Product;
import com.ms.product_service.service.ProductCacheService;
import com.ms.product_service.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    private final ProductService productService;
    private final ProductCacheService productCacheService;

    @Autowired
    public ProductController(ProductService productService, ProductCacheService productCacheService) {
        this.productService = productService;
        this.productCacheService = productCacheService;
    }

    @GetMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<List<ProductResponse>> getProducts() {
        return new ResponseEntity<>(productCacheService.getProducts(), HttpStatus.ACCEPTED);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<ProductResponse> getProductById(@PathVariable String id) {
        return new ResponseEntity<>(productCacheService.getProductById(id), HttpStatus.ACCEPTED);
    }

    @GetMapping("/category/{category}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<List<ProductResponse>> getProductsByCategory(@PathVariable String category) {
        return new ResponseEntity<>(productCacheService.getProductByCategory(category), HttpStatus.ACCEPTED);
    }

    @GetMapping("/{id}/available")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<Boolean> isAvailable(@PathVariable String id, @RequestParam int amount) {
        return new ResponseEntity<>(productService.isAvailable(id, amount), HttpStatus.ACCEPTED);
    }

    @GetMapping("/{id}/price")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<BigDecimal> getPrice(@PathVariable String id) {
        return new ResponseEntity<>(productCacheService.getPrice(id), HttpStatus.ACCEPTED);
    }

    @GetMapping("/{id}/name")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<String> getProductName(@PathVariable String id) {
        return new ResponseEntity<>(productCacheService.getName(id), HttpStatus.ACCEPTED);
    }

    @PostMapping("/stock")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ProductResponse> addProduct(@RequestBody ProductRequest productRequest, @RequestHeader("X-USER-ROLE") String role, @RequestHeader("X-USER-EMAIL") String email) {
        return new ResponseEntity<>(productService.addProduct(productRequest, role), HttpStatus.ACCEPTED);
    }

    @PutMapping("{id}/stock")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable String id, @RequestBody ProductRequest productRequest, @RequestHeader("X-USER-ROLE") String role) {
        return new ResponseEntity<>(productService.updateProduct(id, productRequest, role), HttpStatus.ACCEPTED);
    }

    @DeleteMapping("{id}/stock")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<String> deleteProductById(@PathVariable String id, @RequestHeader("X-USER-ROLE") String role) {
        productService.deleteProduct(id, role);
        return new ResponseEntity<>("Product deleted successfully !", HttpStatus.ACCEPTED);
    }
}

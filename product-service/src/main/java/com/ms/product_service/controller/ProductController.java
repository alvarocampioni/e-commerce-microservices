package com.ms.product_service.controller;

import com.ms.product_service.dto.ProductRequest;
import com.ms.product_service.dto.ProductResponse;
import com.ms.product_service.service.ProductCacheService;
import com.ms.product_service.service.ProductService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Description;
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
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<ProductResponse>> getProducts() {
        return new ResponseEntity<>(productCacheService.getProducts(), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<ProductResponse> getProductById(@PathVariable String id) {
        return new ResponseEntity<>(productCacheService.getProductById(id), HttpStatus.OK);
    }

    @GetMapping("/category/{category}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<ProductResponse>> getProductsByCategory(@Parameter(schema = @Schema(allowableValues = {"TOOL", "FOOD", "ELECTRONIC", "CLOTHING"})) @PathVariable String category) {
        return new ResponseEntity<>(productCacheService.getProductByCategory(category), HttpStatus.OK);
    }

    @GetMapping("/{id}/available")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Boolean> isAvailable(@PathVariable String id, @RequestParam int amount) {
        return new ResponseEntity<>(productService.isAvailable(id, amount), HttpStatus.OK);
    }

    @GetMapping("/{id}/price")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<BigDecimal> getPrice(@PathVariable String id) {
        return new ResponseEntity<>(productCacheService.getPrice(id), HttpStatus.OK);
    }

    @GetMapping("/{id}/name")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> getProductName(@PathVariable String id) {
        return new ResponseEntity<>(productCacheService.getName(id), HttpStatus.OK);
    }

    @PostMapping("/stock")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ProductResponse> addProduct(@RequestBody ProductRequest productRequest, HttpServletRequest request) {
        String role = request.getHeader("X-USER-ROLE");
        return new ResponseEntity<>(productService.addProduct(productRequest, role), HttpStatus.CREATED);
    }

    @PutMapping("/{id}/stock")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<ProductResponse> updateProduct(@PathVariable String id, @RequestBody ProductRequest productRequest, HttpServletRequest request) {
        String role = request.getHeader("X-USER-ROLE");
        return new ResponseEntity<>(productService.updateProduct(id, productRequest, role), HttpStatus.OK);
    }

    @DeleteMapping("/{id}/stock")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<String> deleteProductById(@PathVariable String id, HttpServletRequest request) {
        String role = request.getHeader("X-USER-ROLE");
        productService.deleteProduct(id, role);
        return new ResponseEntity<>("Product deleted successfully !", HttpStatus.OK);
    }
}

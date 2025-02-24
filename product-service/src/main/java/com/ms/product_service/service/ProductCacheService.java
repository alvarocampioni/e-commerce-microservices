package com.ms.product_service.service;

import com.ms.product_service.dto.ProductCategory;
import com.ms.product_service.dto.ProductResponse;
import com.ms.product_service.exception.ResourceNotFoundException;
import com.ms.product_service.model.Product;
import com.ms.product_service.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ProductCacheService {

    private final ProductRepository productRepository;

    @Autowired
    public ProductCacheService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }


    @Cacheable(value = "product", key = "'all'")
    public List<ProductResponse> getProducts() {
        log.info("getProducts called -- accessing database");
        List<Product> products = productRepository.findAll();
        return products.stream().map(this::mapProductToResponse).toList();
    }

    @Cacheable(value = "product", key = "#id")
    public ProductResponse getProductById(String id) {
        log.info("getProductById called -- accessing database");
        Product product = productRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
        return mapProductToResponse(product);
    }

    @Cacheable(value = "product-cat", key = "#category.toUpperCase()")
    public List<ProductResponse> getProductByCategory(String category) {
        log.info("getProductByCategory called -- accessing database");
        ProductCategory productCategory = ProductCategory.fromString(category);
        List<Product> products = productRepository.findByCategory(productCategory);
        return products.stream().map(this::mapProductToResponse).toList();

    }

    @Cacheable(value = "product-price", key = "#id")
    public BigDecimal getPrice(String id) {
        log.info("getPrice called -- accessing database");
        Optional<Product> product = productRepository.findById(id);
        return product.map(Product::getPrice).orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
    }

    @Cacheable(value = "product-name", key = "#id")
    public String getName(String id){
        log.info("getName called -- accessing database");
        Optional<Product> product = productRepository.findById(id);
        return product.map(Product::getName).orElseThrow(() -> new ResourceNotFoundException("Product not found with ID: " + id));
    }

    public ProductResponse mapProductToResponse(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getPrice(), product.getAmount(), product.getCategory());
    }
}

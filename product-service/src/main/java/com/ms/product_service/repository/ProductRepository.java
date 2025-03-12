package com.ms.product_service.repository;

import com.ms.product_service.dto.ProductCategory;
import com.ms.product_service.model.Product;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    List<Product> findByCategory(ProductCategory category);
    boolean existsByIdAndAmountIsGreaterThanEqual(String id, int amount);

    @CacheEvict(value = "product", allEntries = true)
    void deleteAll();
}

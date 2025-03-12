package com.ms.comment_service.service;

import com.ms.comment_service.model.Product;
import com.ms.comment_service.repository.CommentRepository;
import com.ms.comment_service.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CommentRepository commentRepository;

    @Autowired
    public ProductService(ProductRepository productRepository, CommentRepository commentRepository) {
        this.productRepository = productRepository;
        this.commentRepository = commentRepository;
    }

    public void addProduct(Product product) {
        productRepository.save(product);
    }

    public void deleteProduct(Product product) {
        productRepository.delete(product);
        commentRepository.deleteByProductId(product.getProductId());
    }

    public boolean isAvailable(String productId) {
        return productRepository.existsById(productId);
    }
}

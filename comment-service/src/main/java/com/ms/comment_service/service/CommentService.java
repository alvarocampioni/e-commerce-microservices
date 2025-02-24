package com.ms.comment_service.service;

import com.ms.comment_service.client.ProductClient;
import com.ms.comment_service.dto.CommentDTO;
import com.ms.comment_service.exception.ResourceNotFoundException;
import com.ms.comment_service.model.Comment;
import com.ms.comment_service.repository.CommentRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Objects;
import java.util.Optional;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final CacheManager cacheManager;
    private final ProductClient productClient;

    @Autowired
    public CommentService(CommentRepository commentRepository, CacheManager cacheManager, ProductClient productClient) {
        this.commentRepository = commentRepository;
        this.cacheManager = cacheManager;
        this.productClient = productClient;
    }

    @Transactional
    @CircuitBreaker(name = "comment", fallbackMethod = "fallback")
    @Caching(evict = {
            @CacheEvict(value = "comment-user", key = "#customerId"),
            @CacheEvict(value = "comment-specific", key = "#customerId+#productId"),
            @CacheEvict(value = "comment-product", key = "#productId"),
    })
    public void postComment(String customerId, String productId, CommentDTO commentDTO) {
        if(productClient.isAvailable(productId, 0)) {
            Comment comment = new Comment();
            comment.setProductId(productId);
            comment.setCustomerId(customerId);
            comment.setContent(commentDTO.content());
            comment.setPostDate(new Date());
            comment.setUpdateDate(new Date());
            commentRepository.save(comment);
        } else {
            throw new ResourceNotFoundException("Product not found");
        }
    }

    public void fallback(String productId, Throwable throwable) {
        throw new ResourceNotFoundException("Could not fetch the product with ID: " + productId + " to place the comment.");
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "comment-user", key = "#customerId"),
    })
    public void deleteCommentByCustomerId(String customerId) {
        Optional<Comment> comment = commentRepository.findById(customerId);
        if(comment.isPresent() && customerId.equals(comment.get().getCustomerId())){
            String productId = comment.get().getProductId();
            String id = comment.get().getId();
            if(productId != null) {
                Objects.requireNonNull(cacheManager.getCache("comment-specific")).evict(customerId+productId);
                Objects.requireNonNull(cacheManager.getCache("comment-product")).evict(productId);
                Objects.requireNonNull(cacheManager.getCache("comment-id")).evict(id);
            }
            commentRepository.deleteByCustomerId(customerId);
        } else {
            throw new ResourceNotFoundException("Comment not found");
        }
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "comment-user", key = "#customerId"),
            @CacheEvict(value = "comment-id", key = "#id"),
    })
    public void deleteCommentById(String customerId, String id){
        Optional<Comment> comment = commentRepository.findById(id);
        if(comment.isPresent() && customerId.equals(comment.get().getCustomerId())){
            String productId = comment.get().getProductId();
            if(productId != null) {
                Objects.requireNonNull(cacheManager.getCache("comment-specific")).evict(customerId+productId);
                Objects.requireNonNull(cacheManager.getCache("comment-product")).evict(productId);
            }
            commentRepository.deleteById(id);
        } else {
            throw new ResourceNotFoundException("Comment not found");
        }
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "comment-user", key = "#customerId"),
            @CacheEvict(value = "comment-id", key = "#id"),
    })
    public void updateCommentByCommentId(String customerId, String id, CommentDTO commentDTO) {
        Optional<Comment> comment = commentRepository.findById(id);
        if(comment.isPresent() && customerId.equals(comment.get().getCustomerId())) {
            Comment updatedComment = comment.get();
            updatedComment.setContent(commentDTO.content());
            updatedComment.setUpdateDate(new Date());
            String productId = updatedComment.getProductId();
            if(productId != null) {
                Objects.requireNonNull(cacheManager.getCache("comment-specific")).evict(customerId+productId);
                Objects.requireNonNull(cacheManager.getCache("comment-product")).evict(productId);
            }
            commentRepository.save(updatedComment);
        } else {
            throw new ResourceNotFoundException("Comment not found");
        }
    }
}

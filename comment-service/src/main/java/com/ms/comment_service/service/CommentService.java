package com.ms.comment_service.service;

import com.ms.comment_service.dto.CommentDTO;
import com.ms.comment_service.exception.ResourceNotFoundException;
import com.ms.comment_service.exception.UnauthorizedException;
import com.ms.comment_service.model.Comment;
import com.ms.comment_service.repository.CommentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final CacheManager cacheManager;
    private final ProductService productService;

    @Autowired
    public CommentService(CommentRepository commentRepository, CacheManager cacheManager, ProductService productService) {
        this.commentRepository = commentRepository;
        this.cacheManager = cacheManager;
        this.productService = productService;
    }

    @Caching(evict = {
            @CacheEvict(value = "comment-user", key = "#email"),
            @CacheEvict(value = "comment-specific", key = "#email+#productId"),
            @CacheEvict(value = "comment-product", key = "#productId"),
    })
    public void postComment(String email, String productId, CommentDTO commentDTO) {
        if (!isAvailable(productId)) {
            throw new ResourceNotFoundException("Product not found");
        }
        Comment comment = new Comment();
        comment.setProductId(productId);
        comment.setEmail(email);
        comment.setContent(commentDTO.content());
        comment.setPostDate(new Date());
        comment.setUpdateDate(new Date());
        commentRepository.save(comment);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "comment-user", key = "#email"),
    })
    public void deleteCommentByEmail(String email) {
        List<Comment> comments = commentRepository.findByEmail(email);
        for(Comment comment : comments) {
            String productId = comment.getProductId();
            String id = comment.getId();
            Objects.requireNonNull(cacheManager.getCache("comment-specific")).evict(email+productId);
            Objects.requireNonNull(cacheManager.getCache("comment-product")).evict(productId);
            Objects.requireNonNull(cacheManager.getCache("comment-id")).evict(id);
        }
        commentRepository.deleteByEmail(email);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "comment-user", key = "#email"),
            @CacheEvict(value = "comment-id", key = "#id"),
    })
    public void deleteOwnCommentById(String email, String id){
        Optional<Comment> comment = commentRepository.findById(id);
        if(comment.isPresent() && email.equals(comment.get().getEmail())){
            String productId = comment.get().getProductId();
            if(productId != null) {
                Objects.requireNonNull(cacheManager.getCache("comment-specific")).evict(email+productId);
                Objects.requireNonNull(cacheManager.getCache("comment-product")).evict(productId);
            }
            commentRepository.deleteById(id);
        } else {
            throw new ResourceNotFoundException("Comment not found");
        }
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "comment-id", key = "#id"),
    })
    public void deleteAnyCommentById(String role, String id){
        if(!role.equals("ADMIN")) {
            throw new UnauthorizedException("Unauthorized to perform this action");
        }

        Optional<Comment> comment = commentRepository.findById(id);
        if(comment.isPresent()) {
            String productId = comment.get().getProductId();
            String email = comment.get().getEmail();
            if (productId != null && email != null) {
                Objects.requireNonNull(cacheManager.getCache("comment-user")).evict(email);
                Objects.requireNonNull(cacheManager.getCache("comment-specific")).evict(email+productId);
                Objects.requireNonNull(cacheManager.getCache("comment-product")).evict(productId);
            }
            commentRepository.deleteById(id);
        } else {
            throw new ResourceNotFoundException("Comment not found");
        }
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "comment-user", key = "#email"),
            @CacheEvict(value = "comment-id", key = "#id"),
    })
    public void updateCommentByCommentId(String email, String id, CommentDTO commentDTO) {
        Optional<Comment> comment = commentRepository.findById(id);
        if(comment.isPresent() && email.equals(comment.get().getEmail())) {
            Comment updatedComment = comment.get();
            updatedComment.setContent(commentDTO.content());
            updatedComment.setUpdateDate(new Date());
            String productId = updatedComment.getProductId();
            if(productId != null) {
                Objects.requireNonNull(cacheManager.getCache("comment-specific")).evict(email+productId);
                Objects.requireNonNull(cacheManager.getCache("comment-product")).evict(productId);
            }
            commentRepository.save(updatedComment);
        } else {
            throw new ResourceNotFoundException("Comment not found");
        }
    }

    @Transactional
    @CacheEvict(value = "comment-product", key = "#productId")
    public void deleteCommentByProductId(String productId) {
        List<String> affectedCustomersCache = commentRepository.findEmailsByProductId(productId);
        List<String> affectedCommentsCache = commentRepository.findIdsByProductId(productId);

        commentRepository.deleteByProductId(productId);

        affectedCustomersCache.forEach(email -> {
            Objects.requireNonNull(cacheManager.getCache("comment-specific")).evict(email + productId);
            Objects.requireNonNull(cacheManager.getCache("comment-user")).evict(email);
        });
        affectedCommentsCache.forEach(id -> Objects.requireNonNull(cacheManager.getCache("comment-id")).evict(id));
    }

    private boolean isAvailable(String productId){
        return productService.isAvailable(productId);
    }
}

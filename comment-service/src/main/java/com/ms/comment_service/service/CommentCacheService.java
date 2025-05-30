package com.ms.comment_service.service;

import com.ms.comment_service.exception.ResourceNotFoundException;
import com.ms.comment_service.model.Comment;
import com.ms.comment_service.repository.CommentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class CommentCacheService {

    private final CommentRepository commentRepository;

    @Autowired
    public CommentCacheService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    @Cacheable(value = "comment-user", key = "#email")
    public List<Comment> getCommentsByEmail(String email) {
        log.info("getCommentsByEmail called -- accessing database");
        return commentRepository.findByEmail(email);
    }

    @Cacheable(value = "comment-product", key = "#productId")
    public List<Comment> getCommentsByProductId(String productId) {
        log.info("getCommentsByProductId called -- accessing database");
        return commentRepository.findByProductId(productId);
    }

    @Cacheable(value = "comment-specific", key = "#email+#productId")
    public List<Comment> getCommentsByEmailAndProductId(String email, String productId) {
        log.info("getCommentsByEmailAndProductId called -- accessing database");
        return commentRepository.findByEmailAndProductId(email, productId);
    }

    @Cacheable(value = "comment-id", key = "#commentId")
    public Comment getCommentById(String commentId) {
        log.info("getCommentById called -- accessing database");
        Optional<Comment> comment = commentRepository.findById(commentId);
        if(comment.isEmpty()){
            throw new ResourceNotFoundException("No comment found for ID: " + commentId);
        }
        return comment.get();
    }
}

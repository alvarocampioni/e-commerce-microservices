package com.ms.comment_service.repository;

import com.ms.comment_service.model.Comment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends MongoRepository<Comment, String> {
    List<Comment> findByCustomerId(String customerId);
    void deleteByCustomerId(String customerId);
    List<Comment> findByProductId(String productId);
    List<Comment> findByCustomerIdAndProductId(String customerId, String productId);
}

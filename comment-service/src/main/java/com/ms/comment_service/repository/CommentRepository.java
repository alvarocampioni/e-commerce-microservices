package com.ms.comment_service.repository;

import com.ms.comment_service.model.Comment;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends MongoRepository<Comment, String> {
    List<Comment> findByEmail(String email);
    void deleteByEmail(String email);
    void deleteByProductId(String productId);
    List<Comment> findByProductId(String productId);
    List<Comment> findByEmailAndProductId(String email, String productId);

    //fetch matching productId without _id
    @Query(value = "{ 'productId': ?0 }", fields = "{ 'customerId': 1, '_id': 0 }")
    List<String> findEmailsByProductId(String productId);

    //fetch matching productId with only _id
    @Query(value = "{ 'productId': ?0 }", fields = "{ '_id': 1 }")
    List<String> findIdsByProductId(String productId);

    @CacheEvict(value = "*", allEntries = true)
    void deleteAll();
}

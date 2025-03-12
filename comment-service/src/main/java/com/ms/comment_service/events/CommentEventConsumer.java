package com.ms.comment_service.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.comment_service.model.Product;
import com.ms.comment_service.service.CommentService;
import com.ms.comment_service.service.ProductService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class CommentEventConsumer {

    private final CommentService commentService;
    private final ProductService productService;
    private final ObjectMapper objectMapper;

    @Autowired
    public CommentEventConsumer(CommentService commentService, ProductService productService, ObjectMapper objectMapper) {
        this.commentService = commentService;
        this.productService = productService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "user-deleted", groupId = "comment-deleted")
    public void deleteComments(ConsumerRecord<String, String> record) {
        commentService.deleteCommentByCustomerId(record.value());
    }

    @KafkaListener(topics = {"created-product", "deleted-product"}, groupId = "comment-products")
    public void receiveProductEvents(ConsumerRecord<String, String> record) {
        try {
            String json = record.value();
            Product product = objectMapper.readValue(json, Product.class);

            String topic = record.topic();
            switch (topic) {
                case "created-product":
                    productService.addProduct(product);
                    break;
                case "deleted-product":
                    productService.deleteProduct(product);
                    commentService.deleteCommentByProductId(product.getProductId());
                    break;
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}


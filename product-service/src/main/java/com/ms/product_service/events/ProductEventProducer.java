package com.ms.product_service.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.product_service.dto.OrderDTO;
import com.ms.product_service.dto.ProductResponse;
import com.ms.product_service.dto.RejectOrderDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ProductEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String acceptedOrderTopic = "accepted-order";
    private final String rejectedOrderTopic = "rejected-order";
    private final String createdProductTopic = "created-product";
    private final String updatedProductTopic = "updated-product";
    private final String deletedProductTopic = "deleted-product";

    @Autowired
    public ProductEventProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void createdProduct(ProductResponse product) {
        String json = writeValueAsString(product);
        kafkaTemplate.send(createdProductTopic, json);
    }

    public void updatedProduct(ProductResponse product) {
        String json = writeValueAsString(product);
        kafkaTemplate.send(updatedProductTopic, json);
    }

    public void deletedProduct(ProductResponse product) {
        String json = writeValueAsString(product);
        kafkaTemplate.send(deletedProductTopic, json);
    }

    public void acceptedOrder(OrderDTO order){
        String json = writeValueAsString(order);
        kafkaTemplate.send(acceptedOrderTopic, json);
    }

    public void rejectedOrder(RejectOrderDTO rejectOrderDTO){
        String json = writeValueAsString(rejectOrderDTO);
        kafkaTemplate.send(rejectedOrderTopic, json);
    }

    private <T> String writeValueAsString(T value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error while writing value to JSON", e);
        }
    }
}

package com.ms.product_service.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.product_service.dto.OrderDTO;
import com.ms.product_service.service.ProductService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class ProductEventConsumer {

    private final ProductService productService;
    private final ObjectMapper objectMapper;

    @Autowired
    public ProductEventConsumer(ProductService productService, ObjectMapper objectMapper) {
        this.productService = productService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "stock-recovered", groupId = "stock_recover")
    public void receiveRecoverRequest(ConsumerRecord<String, String> record) {
        try {
            String json = record.value();
            OrderDTO orderDTO = objectMapper.readValue(json, OrderDTO.class);
            productService.recoverStock(orderDTO);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "stock-deducted", groupId = "stock_deduct")
    public void receiveDeduceRequest(ConsumerRecord<String, String> record) {
        try {
            String json = record.value();
            OrderDTO orderDTO = objectMapper.readValue(json, OrderDTO.class);
            productService.deductProduct(orderDTO);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}

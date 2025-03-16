package com.ms.order_service.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.order_service.dto.OrderDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public OrderEventProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void createdOrder(String email) {
        kafkaTemplate.send("created-order", email);
    }

    public void checkOrder(OrderDTO order) {
        String json = parseObjectToJson(order);
        kafkaTemplate.send("check-order", json);
    }

    public void canceledOrder(String orderId) {
        kafkaTemplate.send("canceled-order", orderId);
    }

    public void recoveredStock(OrderDTO order){
        String json = parseObjectToJson(order);
        kafkaTemplate.send("recovered-stock", json);
    }

    private <T> String parseObjectToJson(T order) {
        try {
            return objectMapper.writeValueAsString(order);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}

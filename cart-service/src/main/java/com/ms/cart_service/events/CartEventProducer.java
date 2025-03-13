package com.ms.cart_service.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.cart_service.dto.CartDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class CartEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String loadedOrder = "loaded-order";

    @Autowired
    public CartEventProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendOrder(CartDTO cart) {
        try {
            String json = objectMapper.writeValueAsString(cart);
            kafkaTemplate.send(loadedOrder, json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}

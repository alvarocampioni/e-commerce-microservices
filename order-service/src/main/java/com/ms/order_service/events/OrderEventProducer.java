package com.ms.order_service.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.order_service.dto.OrderCanceledDTO;
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

    public void requestedOrder(String customerId) {
        kafkaTemplate.send("requested-order", customerId);
    }

    public void createdOrder(String customerId) {
        kafkaTemplate.send("created-order", customerId);
    }

    public void canceledOrder(OrderCanceledDTO orderCanceledDTO) {
        try {
            String json = objectMapper.writeValueAsString(orderCanceledDTO);
            kafkaTemplate.send("canceled-order", json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void requestedCancelPayment(String orderId) {
        kafkaTemplate.send("requested-cancel-payment", orderId);
    }

    public void sendOrderData(OrderDTO order) {
        try {
            String json = objectMapper.writeValueAsString(order);
            kafkaTemplate.send("order-placed", json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void requestDeductStock(OrderDTO order){
        try {
            String json = objectMapper.writeValueAsString(order);
            kafkaTemplate.send("stock-deducted", json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void requestRecoverStock(OrderDTO order){
        try {
            String json = objectMapper.writeValueAsString(order);
            kafkaTemplate.send("stock-recovered", json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}

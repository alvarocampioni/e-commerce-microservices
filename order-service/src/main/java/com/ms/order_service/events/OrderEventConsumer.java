package com.ms.order_service.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.order_service.dto.CartDTO;
import com.ms.order_service.dto.PaymentStatusChangedDTO;
import com.ms.order_service.model.OrderStatus;
import com.ms.order_service.service.OrderService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class OrderEventConsumer {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    public OrderEventConsumer(OrderService orderService, ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "order-confirmed", groupId = "confirm")
    public void receiveOrderData(ConsumerRecord<String, String> message) {
        try {
            String json = message.value();
            CartDTO cart = objectMapper.readValue(json, CartDTO.class);
            orderService.placeCartOrder(cart);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "succeeded-payment", groupId = "success")
    public void receiveSuccess(ConsumerRecord<String, String> message) {
        try {
            String json = message.value();
            PaymentStatusChangedDTO status = objectMapper.readValue(json, PaymentStatusChangedDTO.class);
            orderService.confirmOrder(status.orderId());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "failed-payment", groupId = "fail")
    public void receiveFail(ConsumerRecord<String, String> message) {
        try {
            String json = message.value();
            PaymentStatusChangedDTO status = objectMapper.readValue(json, PaymentStatusChangedDTO.class);
            orderService.revokeOrder(status.orderId(), OrderStatus.FAILED);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}

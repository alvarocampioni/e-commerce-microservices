package com.ms.order_service.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.order_service.dto.CartDTO;
import com.ms.order_service.dto.OrderDTO;
import com.ms.order_service.dto.PaymentStatusChangedDTO;
import com.ms.order_service.dto.RejectOrderDTO;
import com.ms.order_service.model.OrderStatus;
import com.ms.order_service.service.OrderService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class OrderEventConsumer {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @Autowired
    public OrderEventConsumer(OrderService orderService, ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "accepted-order", groupId = "accepted")
    public void acceptOrder(ConsumerRecord<String, String> message){
        try {
            String json = message.value();
            OrderDTO orderDTO = objectMapper.readValue(json, OrderDTO.class);
            orderService.acceptOrder(orderDTO);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "rejected-order", groupId = "rejected")
    public void rejectOrder(ConsumerRecord<String, String> message){
        try {
            String json = message.value();
            RejectOrderDTO rejectOrderDTO = objectMapper.readValue(json, RejectOrderDTO.class);
            orderService.updateOrderStatus(rejectOrderDTO.orderId(), OrderStatus.FAILED);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "loaded-order", groupId = "load")
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
            PaymentStatusChangedDTO changedDTO = objectMapper.readValue(json, PaymentStatusChangedDTO.class);
            orderService.updateOrderStatus(changedDTO.orderId(), OrderStatus.SUCCESSFUL);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "failed-payment", groupId = "fail")
    public void receiveFail(ConsumerRecord<String, String> message) {
        try {
            String json = message.value();
            PaymentStatusChangedDTO changedDTO = objectMapper.readValue(json, PaymentStatusChangedDTO.class);
            orderService.updateOrderStatus(changedDTO.orderId(), OrderStatus.FAILED);
            orderService.recoverStock(changedDTO.orderId());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}

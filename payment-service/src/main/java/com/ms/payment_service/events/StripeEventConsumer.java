package com.ms.payment_service.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.payment_service.dto.OrderDTO;
import com.ms.payment_service.service.StripeService;
import com.stripe.exception.StripeException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class StripeEventConsumer {

    private final StripeService stripeService;
    private final ObjectMapper objectMapper;

    @Autowired
    public StripeEventConsumer(StripeService stripeService, ObjectMapper objectMapper) {
        this.stripeService = stripeService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "accepted-order", groupId = "stripe")
    public void receiveOrderData(ConsumerRecord<String, String> record) throws StripeException {
        try {
            String json = record.value();
            OrderDTO orderDTO = objectMapper.readValue(json, OrderDTO.class);
            stripeService.processOrderCreation(orderDTO);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "canceled-order", groupId = "stripe")
    public void receiveCancelRequest(ConsumerRecord<String, String> record) throws StripeException {
        stripeService.cancelPayment(record.value());
    }
}

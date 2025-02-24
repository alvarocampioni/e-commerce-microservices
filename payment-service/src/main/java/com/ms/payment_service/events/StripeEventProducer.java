package com.ms.payment_service.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.payment_service.dto.PaymentCreatedDTO;
import com.ms.payment_service.dto.PaymentStatusChangedDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class StripeEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public StripeEventProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void paymentCreated(PaymentCreatedDTO dto){
        try {
            String json = objectMapper.writeValueAsString(dto);
            kafkaTemplate.send("created-payment", json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void paymentFailed(PaymentStatusChangedDTO dto){
        try {
            String json = objectMapper.writeValueAsString(dto);
            kafkaTemplate.send("failed-payment", json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void paymentSucceeded(PaymentStatusChangedDTO dto){
        try {
            String json = objectMapper.writeValueAsString(dto);
            kafkaTemplate.send("succeeded-payment", json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void paymentCanceled(PaymentStatusChangedDTO dto){
        try {
            String json = objectMapper.writeValueAsString(dto);
            kafkaTemplate.send("canceled-payment", json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}

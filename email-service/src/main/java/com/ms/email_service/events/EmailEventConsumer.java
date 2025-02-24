package com.ms.email_service.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.email_service.dto.*;
import com.ms.email_service.service.EmailService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class EmailEventConsumer {

    private final EmailService emailService;
    private final ObjectMapper jacksonObjectMapper;

    @Autowired
    public EmailEventConsumer(EmailService emailService, ObjectMapper jacksonObjectMapper) {
        this.emailService = emailService;
        this.jacksonObjectMapper = jacksonObjectMapper;
    }

    @KafkaListener(topics = "notify-user", groupId = "user")
    public void notifyUser(ConsumerRecord<String, String> record) {
        try {
            String json = record.value();
            UserNotificationDTO userNotificationDTO = jacksonObjectMapper.readValue(json, UserNotificationDTO.class);
            emailService.sendUserNotification(userNotificationDTO);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "created-payment", groupId = "payment-creation")
    public void createdPayment(ConsumerRecord<String, String> record) {
        try {
            String json = record.value();
            PaymentCreatedDTO paymentCreatedDTO = jacksonObjectMapper.readValue(json, PaymentCreatedDTO.class);
            emailService.paymentCreatedEmail(paymentCreatedDTO);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = {"succeeded-payment", "failed-payment", "canceled-payment"}, groupId = "payment-status-change")
    public void succeededPayment(ConsumerRecord<String, String> record) {
        try {
            String json = record.value();
            String topic = record.topic();
            PaymentStatusChangedDTO paymentStatusChangedDTO = jacksonObjectMapper.readValue(json, PaymentStatusChangedDTO.class);
            switch (topic){
                case "succeeded-payment":
                    emailService.paymentSucceededEmail(paymentStatusChangedDTO);
                    break;
                case "failed-payment":
                    emailService.paymentFailedEmail(paymentStatusChangedDTO);
                    break;
                case "canceled-payment":
                    emailService.paymentCanceledEmail(paymentStatusChangedDTO);
                    break;
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "order-confirmed", groupId = "order-request")
    public void requestedOrder(ConsumerRecord<String, String> record) {
        try {
            String json = record.value();
            CartDTO cartDTO = jacksonObjectMapper.readValue(json, CartDTO.class);
            emailService.orderRequestedEmail(cartDTO);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @KafkaListener(topics = "canceled-order", groupId = "order-cancel")
    public void canceledOrder(ConsumerRecord<String, String> record) {
        try {
            String json = record.value();
            OrderCanceledDTO orderCanceledDTO = jacksonObjectMapper.readValue(json, OrderCanceledDTO.class);
            emailService.orderCanceledEmail(orderCanceledDTO);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }



}

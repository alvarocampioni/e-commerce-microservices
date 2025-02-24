package com.ms.user_service.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.user_service.dto.UserNotificationDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class UserEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String userTopic = "notify-user";

    @Autowired
    public UserEventProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendUserNotification(UserNotificationDTO userNotificationDTO){
        try {
            String json = objectMapper.writeValueAsString(userNotificationDTO);
            kafkaTemplate.send(userTopic, json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}

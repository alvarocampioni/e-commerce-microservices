package com.ms.user_service.dto;

import java.io.Serializable;

public record UserNotificationDTO (String email, String subject, String content) implements Serializable {
}

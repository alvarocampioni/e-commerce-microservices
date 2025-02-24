package com.ms.comment_service.dto;

import java.time.LocalDateTime;

public record ErrorResponse(String error, String message, int status, LocalDateTime timestamp) {
}

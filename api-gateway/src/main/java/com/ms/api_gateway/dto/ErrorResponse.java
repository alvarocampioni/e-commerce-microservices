package com.ms.api_gateway.dto;

import java.time.LocalDateTime;

public record ErrorResponse (String error, String message, int status, LocalDateTime timestamp) {
}

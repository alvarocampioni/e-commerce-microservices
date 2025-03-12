package com.ms.user_service.dto;

import java.io.Serializable;

public record TokenValidationDTO(String subject, String role) implements Serializable {
}

package com.ms.user_service.dto;

import java.io.Serializable;

public record UserConfirmationDTO (String email, String code) implements Serializable {
}

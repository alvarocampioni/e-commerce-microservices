package com.ms.email_service.dto;

import java.io.Serializable;
import java.util.List;

public record CartDTO (List<CartProduct> cart) implements Serializable {
}

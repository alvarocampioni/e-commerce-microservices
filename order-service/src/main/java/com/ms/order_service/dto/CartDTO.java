package com.ms.order_service.dto;

import java.io.Serializable;
import java.util.List;

public record CartDTO (List<CartProductDTO> cart) implements Serializable {
}

package com.ms.payment_service.dto;

import java.io.Serializable;
import java.util.List;

public record OrderDTO (List<OrderProduct> order) implements Serializable {
}
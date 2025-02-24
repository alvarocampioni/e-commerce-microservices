package com.ms.order_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Entity
@Table(name = "t_order")
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(OrderProductId.class)
public class OrderProduct implements Serializable {

    @Id
    private String id;
    @Id
    private String customerId;
    @Id
    private String productId;

    private String productName;
    private int amount;
    private OrderStatus status;
    private BigDecimal price;
    private boolean isArchived;
    @CreationTimestamp
    private Timestamp order_date;
    private Timestamp execution_date;
}

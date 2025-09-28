package com.food.Nest.orders.model;

import com.food.Nest.orders.model.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class OrderEvent {
    private Long orderId;
    private OrderStatus status;
    private LocalDateTime timestamp;
    private String customerEmail;
    private String customerName;
    private String eventType;
}

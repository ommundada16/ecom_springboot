package com.ecommerce.ecom_proj.dto;

import com.ecommerce.ecom_proj.model.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(Long id, String status, BigDecimal totalAmount, LocalDateTime createdAt,
                            List<OrderItemResponse> items) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getStatus().name(), order.getTotalAmount(),
                order.getCreatedAt(), order.getItems().stream().map(OrderItemResponse::from).toList());
    }
}

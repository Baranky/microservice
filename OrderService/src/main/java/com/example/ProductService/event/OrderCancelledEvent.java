package com.example.ProductService.event;

public record OrderCancelledEvent(
        Long orderId,
        String reason
) {}

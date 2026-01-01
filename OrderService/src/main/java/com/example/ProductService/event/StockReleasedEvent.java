package com.example.ProductService.event;

public record StockReleasedEvent(
        Long orderId,
        Long productId,
        Integer quantity,
        String reason
) {}

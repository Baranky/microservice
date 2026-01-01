package com.example.InventoryService.event;

public record StockReleasedEvent(
        Long orderId,
        Long productId,
        Integer quantity,
        String reason
) {}

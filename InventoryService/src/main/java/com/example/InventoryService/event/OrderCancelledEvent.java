package com.example.InventoryService.event;

public record OrderCancelledEvent(
        Long orderId,
        String reason
) {}

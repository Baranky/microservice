package com.example.ProductService.event;

import java.math.BigDecimal;

public record OrderEvent(
        Long orderId,
        Long productId,
        Integer quantity,
        BigDecimal totalPrice,
        String customerEmail
) {}

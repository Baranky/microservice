package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.demo.enums.PaymentMethod;
import com.example.demo.enums.PaymentStatus;

public record PaymentResponse(
        String id,
        String orderId,
        String userId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        PaymentMethod paymentMethod,
        String maskedCardNumber,
        String cardHolderName,
        LocalDateTime createdAt
        ) {

}

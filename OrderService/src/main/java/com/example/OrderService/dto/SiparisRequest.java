package com.example.OrderService.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SiparisRequest {
    private Long productId;
    private Integer quantity;
    private BigDecimal totalPrice;
    private String customerEmail;
}


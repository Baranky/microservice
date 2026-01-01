package com.example.demo.event ;

import java.math.BigDecimal;


public record StockReservedEvent(
           Long orderId ,
           Long productId ,
           Integer quantity,
           BigDecimal totalPrice,
           String customerEmail

) {}
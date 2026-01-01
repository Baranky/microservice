package com.example.demo.event ;

import java.math.BigDecimal ;

public record PaymentConfirmedEvent(
         Long orderId ,
          String paymentId ,
          BigDecimal amount ,
          String status ,
          String customerEmail
) {}

   

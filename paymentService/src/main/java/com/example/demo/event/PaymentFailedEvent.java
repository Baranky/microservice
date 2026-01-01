package com.example.demo.event ;

public record PaymentFailedEvent (
           Long orderId ,
           Long productId ,
           Integer quantity ,
           String paymentId ,
           String reason
){ }

    
    
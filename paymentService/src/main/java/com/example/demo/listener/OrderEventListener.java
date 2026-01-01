package com.example.demo.listener;

import com.example.demo.entity.Payment;
import com.example.demo.event.StockReservedEvent;
import com.example.demo.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    private final PaymentService paymentService;

    public OrderEventListener(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @KafkaListener(topics = "stock-reserved", groupId = "payment-group", containerFactory = "stockReservedKafkaListenerContainerFactory")
    @Transactional
    public void handleStockReserved(StockReservedEvent stockReservedEvent) {
        log.info("Stock reserved event alındı: orderId={}, productId={}, quantity={}, totalPrice={}, customerEmail={}",
                stockReservedEvent.orderId(), stockReservedEvent.productId(), stockReservedEvent.quantity(),
                stockReservedEvent.totalPrice(), stockReservedEvent.customerEmail());

        try {
            // Payment kaydı oluştur (PENDING status ile - manuel ödeme bekleniyor)
            Payment payment = paymentService.createPendingPayment(stockReservedEvent);
            log.info("Payment kaydı oluşturuldu (manuel ödeme bekleniyor): paymentId={}, orderId={}, status=PENDING",
                    payment.getId(), payment.getOrderId());
        } catch (Exception e) {
            log.error("Payment kaydı oluşturulurken hata oluştu: orderId={}",
                    stockReservedEvent.orderId(), e);
            throw e;
        }
    }
}


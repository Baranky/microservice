package com.example.ProductService.listener;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.ProductService.entity.Order;
import com.example.ProductService.enums.OrderStatus;
import com.example.ProductService.event.OrderCancelledEvent;
import com.example.ProductService.event.PaymentConfirmedEvent;
import com.example.ProductService.event.StockReleasedEvent;
import com.example.ProductService.repository.OrderRepository;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class OrderEventListener {

    private final OrderRepository orderRepository;

    public OrderEventListener(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @KafkaListener(topics = "payment-confirmed", groupId = "order-group", containerFactory = "paymentConfirmedKafkaListenerContainerFactory")
    @Transactional
    public void handlePaymentConfirmed(PaymentConfirmedEvent event) {
        log.info("Payment confirmed event alındı: orderId={}, paymentId={}, status={}",
                event.orderId(), event.paymentId(), event.status());

        try {
            Order order = orderRepository.findById(event.orderId())
                    .orElseThrow(() -> new RuntimeException("Sipariş bulunamadı: " + event.orderId()));

            if ("SUCCESS".equalsIgnoreCase(event.status())) {
                order.setStatus(OrderStatus.CONFIRMED);
                orderRepository.save(order);
                log.info("Sipariş onaylandı: orderId={}, status=CONFIRMED", order.getId());
            } else {
                log.warn("Ödeme başarısız, sipariş iptal edilecek: orderId={}, paymentStatus={}",
                        event.orderId(), event.status());
            }
        } catch (Exception e) {
            log.error("Payment confirmed event işlenirken hata oluştu: orderId={}",
                    event.orderId(), e);
            throw e;
        }
    }

    @KafkaListener(topics = "order-cancelled", groupId = "order-group", containerFactory = "orderCancelledKafkaListenerContainerFactory")
    @Transactional
    public void handleOrderCancelled(OrderCancelledEvent event) {
        log.info("Order cancelled event alındı: orderId={}, reason={}",
                event.orderId(), event.reason());

        try {
            Order order = orderRepository.findById(event.orderId())
                    .orElseThrow(() -> new RuntimeException("Sipariş bulunamadı: " + event.orderId()));

            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            log.info("Sipariş iptal edildi: orderId={}, reason={}, status=CANCELLED",
                    order.getId(), event.reason());
        } catch (Exception e) {
            log.error("Order cancelled event işlenirken hata oluştu: orderId={}",
                    event.orderId(), e);
            throw e;
        }
    }

    @KafkaListener(topics = "stock-released", groupId = "order-group", containerFactory = "stockReleasedKafkaListenerContainerFactory")
    @Transactional
    public void handleStockReleased(StockReleasedEvent event) {
        log.info("Stock released event alındı: orderId={}, productId={}, quantity={}, reason={}",
                event.orderId(), event.productId(), event.quantity(), event.reason());

        try {
            Order order = orderRepository.findById(event.orderId())
                    .orElseThrow(() -> new RuntimeException("Sipariş bulunamadı: " + event.orderId()));

            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            log.info("Sipariş iptal edildi (stock released): orderId={}, reason={}, status=CANCELLED",
                    order.getId(), event.reason());
        } catch (Exception e) {
            log.error("Stock released event işlenirken hata oluştu: orderId={}",
                    event.orderId(), e);
            throw e;
        }
    }
}

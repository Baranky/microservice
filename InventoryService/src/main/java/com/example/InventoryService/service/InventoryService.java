package com.example.InventoryService.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.InventoryService.entity.Inventory;
import com.example.InventoryService.event.OrderCancelledEvent;
import com.example.InventoryService.event.OrderEvent;
import com.example.InventoryService.event.PaymentFailedEvent;
import com.example.InventoryService.event.StockReleasedEvent;
import com.example.InventoryService.event.StockReservedEvent;
import com.example.InventoryService.repository.InventoryRepository;


@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);
    private final InventoryRepository inventoryRepository;
    private final KafkaTemplate<String, StockReservedEvent> stockReservedKafkaTemplate;
    private final KafkaTemplate<String, OrderCancelledEvent> orderCancelledKafkaTemplate;
    private final KafkaTemplate<String, StockReleasedEvent> stockReleasedKafkaTemplate;

    public InventoryService(InventoryRepository inventoryRepository,
                           KafkaTemplate<String, StockReservedEvent> stockReservedKafkaTemplate,
                           KafkaTemplate<String, OrderCancelledEvent> orderCancelledKafkaTemplate,
                           KafkaTemplate<String, StockReleasedEvent> stockReleasedKafkaTemplate) {
        this.inventoryRepository = inventoryRepository;
        this.stockReservedKafkaTemplate = stockReservedKafkaTemplate;
        this.orderCancelledKafkaTemplate = orderCancelledKafkaTemplate;
        this.stockReleasedKafkaTemplate = stockReleasedKafkaTemplate;
    }

    public Inventory createInventory(Inventory inventory) {
        Optional<Inventory> existing = inventoryRepository.findByProductId(inventory.getProductId());
        if (existing.isPresent()) {
            throw new RuntimeException("Bu ürün için zaten stok kaydı mevcut: " + inventory.getProductId());
        }
        return inventoryRepository.save(inventory);
    }


    @KafkaListener(topics = "order-placed", groupId = "inventory-group")
    @Transactional
    public void handleOrderPlaced(OrderEvent event) {
        log.info("Order placed event alındı: orderId={}, productId={}, quantity={}, totalPrice={}",
                event.orderId(), event.productId(), event.quantity(), event.totalPrice());

        try {
            Optional<Inventory> invOpt = inventoryRepository.findByProductId(event.productId());
            
            if (invOpt.isEmpty()) {
                log.warn("Stok kaydı bulunamadı, sipariş iptal ediliyor: orderId={}, productId={}",
                        event.orderId(), event.productId());
                
                OrderCancelledEvent cancelledEvent = new OrderCancelledEvent(
                        event.orderId(),
                        "Stok kaydı bulunamadı: productId=" + event.productId()
                );
                orderCancelledKafkaTemplate.send("order-cancelled", cancelledEvent);
                log.info("Order cancelled event gönderildi: orderId={}", event.orderId());
                return;
            }

            Inventory inv = invOpt.get();

            if (inv.getStock() < event.quantity()) {
                log.warn("Yetersiz stok, sipariş iptal ediliyor: orderId={}, productId={}, mevcut={}, istenen={}",
                        event.orderId(), event.productId(), inv.getStock(), event.quantity());
                
                OrderCancelledEvent cancelledEvent = new OrderCancelledEvent(
                        event.orderId(),
                        String.format("Yetersiz stok: mevcut=%d, istenen=%d", inv.getStock(), event.quantity())
                );
                orderCancelledKafkaTemplate.send("order-cancelled", cancelledEvent);
                log.info("Order cancelled event gönderildi: orderId={}", event.orderId());
                return;
            }

            inv.setStock(inv.getStock() - event.quantity());
            inventoryRepository.save(inv);
            log.info("Stok düşüldü: productId={}, yeni stok={}, düşülen miktar={}",
                    inv.getProductId(), inv.getStock(), event.quantity());

            StockReservedEvent stockReservedEvent = new StockReservedEvent(
                    event.orderId(),
                    event.productId(),
                    event.quantity(),
                    event.totalPrice(),
                    event.customerEmail()
            );
            stockReservedKafkaTemplate.send("stock-reserved", stockReservedEvent);
            log.info("Stock reserved event gönderildi: orderId={}, productId={}, quantity={}",
                    event.orderId(), event.productId(), event.quantity());

        } catch (Exception e) {
            log.error("Order placed event işlenirken hata oluştu: orderId={}",
                    event.orderId(), e);
            throw e;
        }
    }


    @KafkaListener(topics = "payment-failed", groupId = "inventory-group", containerFactory = "paymentFailedKafkaListenerContainerFactory")
    @Transactional
    public void handlePaymentFailed(PaymentFailedEvent event) {
        log.info("Payment failed event alındı: orderId={}, productId={}, quantity={}, paymentId={}, reason={}",
                event.orderId(), event.productId(), event.quantity(), event.paymentId(), event.reason());

        try {
            Optional<Inventory> invOpt = inventoryRepository.findByProductId(event.productId());
            if (invOpt.isEmpty()) {
                log.warn("Stok kaydı bulunamadı, stok geri eklenemiyor: orderId={}, productId={}",
                        event.orderId(), event.productId());
                return;
            }

            Inventory inv = invOpt.get();

            inv.setStock(inv.getStock() + event.quantity());
            inventoryRepository.save(inv);
            log.info("Stok geri eklendi: productId={}, yeni stok={}, eklenen miktar={}",
                    inv.getProductId(), inv.getStock(), event.quantity());

            StockReleasedEvent stockReleasedEvent = new StockReleasedEvent(
                    event.orderId(),
                    event.productId(),
                    event.quantity(),
                    "Ödeme başarısız: " + event.reason()
            );
            stockReleasedKafkaTemplate.send("stock-released", stockReleasedEvent);
            log.info("Stock released event gönderildi: orderId={}, productId={}, quantity={}",
                    event.orderId(), event.productId(), event.quantity());

        } catch (Exception e) {
            log.error("Payment failed event işlenirken hata oluştu: orderId={}",
                    event.orderId(), e);
            throw e;
        }
    }
}

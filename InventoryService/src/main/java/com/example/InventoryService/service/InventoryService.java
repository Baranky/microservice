package com.example.InventoryService.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.example.InventoryService.entity.Inventory;
import com.example.InventoryService.event.OrderEvent;
import com.example.InventoryService.repository.InventoryRepository;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @KafkaListener(topics = "order-placed", groupId = "inventory-group")
    public void handleOrderPlaced(OrderEvent event) {
        log.info("Order event alındı: orderId={}, productId={}, quantity={}",
                event.getOrderId(), event.getProductId(), event.getQuantity());

        Optional<Inventory> invOpt = inventoryRepository.findByProductId(event.getProductId());
        if (invOpt.isEmpty()) {
            log.warn("Stok kaydı yok, productId={}", event.getProductId());
            return;
        }

        Inventory inv = invOpt.get();

        if (inv.getStock() < event.getQuantity()) {
            log.warn("Yetersiz stok, productId={}, mevcut={}, istenen={}",
                    event.getProductId(), inv.getStock(), event.getQuantity());
            return;
        }

        inv.setStock(inv.getStock() - event.getQuantity());
        inventoryRepository.save(inv);

        log.info("Stok düşüldü. productId={}, yeni stok={}", inv.getProductId(), inv.getStock());
    }
}

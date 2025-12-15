package com.example.OrderService.controller;

import com.example.OrderService.entity.Order;
import com.example.OrderService.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@Slf4j
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        log.info("Tüm siparişler isteniyor");
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable("id") Long id) {
        log.info("Sipariş isteniyor: {}", id);
        return orderService.getOrderById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/{email}")
    public ResponseEntity<List<Order>> getOrdersByCustomerEmail(@PathVariable("email") String email) {
        log.info("Müşteri siparişleri isteniyor: {}", email);
        return ResponseEntity.ok(orderService.getOrdersByCustomerEmail(email));
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Order order) {
        log.info("Yeni sipariş oluşturuluyor");
        Order createdOrder = orderService.createOrder(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Order> updateOrder(@PathVariable("id") Long id, @RequestBody Order order) {
        log.info("Sipariş güncelleniyor: {}", id);
        try {
            Order updatedOrder = orderService.updateOrder(id, order);
            return ResponseEntity.ok(updatedOrder);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable("id") Long id) {
        log.info("Sipariş durumu güncelleniyor: {} -> {}", id);
        try {
            Order updatedOrder = orderService.updateOrderStatus(id);
            return ResponseEntity.ok(updatedOrder);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable("id") Long id) {
        log.info("Sipariş siliniyor: {}", id);
        try {
            orderService.deleteOrder(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Circuit Breaker test endpoint
    @PostMapping("/test-circuit")
    public ResponseEntity<String> testCircuitBreaker(@RequestParam("productId") Long productId) {
        log.info("Circuit breaker testi, productId={}", productId);
        String result = orderService.siparisVer(productId);
        return ResponseEntity.ok(result);
    }
}

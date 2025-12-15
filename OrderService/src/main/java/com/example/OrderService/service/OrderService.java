package com.example.OrderService.service;

import com.example.OrderService.client.ProductClient;
import com.example.OrderService.entity.Order;
import com.example.OrderService.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;

    public OrderService(OrderRepository orderRepository, ProductClient productClient) {
        this.orderRepository = orderRepository;
        this.productClient = productClient;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public List<Order> getOrdersByCustomerEmail(String email) {
        return orderRepository.findByCustomerEmail(email);
    }

    public Order createOrder(Order order) {
        return orderRepository.save(order);
    }

    public Order updateOrder(Long id, Order order) {
        Order existingOrder = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sipariş bulunamadı: " + id));

        existingOrder.setProductId(order.getProductId());
        existingOrder.setQuantity(order.getQuantity());
        existingOrder.setTotalPrice(order.getTotalPrice());
        existingOrder.setCustomerName(order.getCustomerName());
        existingOrder.setCustomerEmail(order.getCustomerEmail());
        return orderRepository.save(existingOrder);
    }

    public Order updateOrderStatus(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sipariş bulunamadı: " + id));
        return orderRepository.save(order);
    }

    // Dış ürün servisi çağrısı korumalı (Circuit Breaker)
    @CircuitBreaker(name = "productService", fallbackMethod = "urunFallback")
    public String siparisVer(Long productId) {
        // Ürün bilgisini al; hata durumunda fallback çalışacak.
        productClient.getProductById(productId);
        return "Sipariş oluşturuldu, ürün onaylandı.";
    }

    public String urunFallback(Long productId, Throwable t) {
        log.error("Ürün servisi yanıt vermiyor, fallback devrede. productId={}", productId, t);
        return "Ürün servisi şu an cevap vermiyor. Siparişiniz 'BEKLEMEDE' moduna alındı.";
    }

    public void deleteOrder(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new RuntimeException("Sipariş bulunamadı: " + id);
        }
        orderRepository.deleteById(id);
    }
}

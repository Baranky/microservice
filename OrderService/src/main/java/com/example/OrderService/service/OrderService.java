package com.example.OrderService.service;

import com.example.OrderService.entity.Order;
import com.example.OrderService.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<Order> getAllOrders() {
        log.info("Tüm siparişler getiriliyor");
        return orderRepository.findAll();
    }

    public Optional<Order> getOrderById(Long id) {
        log.info("Sipariş getiriliyor: {}", id);
        return orderRepository.findById(id);
    }

    public List<Order> getOrdersByCustomerEmail(String email) {
        log.info("Müşteri siparişleri getiriliyor: {}", email);
        return orderRepository.findByCustomerEmail(email);
    }

    public Order createOrder(Order order) {
        log.info("Yeni sipariş oluşturuluyor: ProductId={}, Quantity={}", order.getProductId(), order.getQuantity());
        return orderRepository.save(order);
    }

    public Order updateOrder(Long id, Order order) {
        log.info("Sipariş güncelleniyor: {}", id);
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
        log.info("Sipariş durumu güncelleniyor: {} -> {}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sipariş bulunamadı: " + id));
        return orderRepository.save(order);
    }

    public void deleteOrder(Long id) {
        log.info("Sipariş siliniyor: {}", id);
        if (!orderRepository.existsById(id)) {
            throw new RuntimeException("Sipariş bulunamadı: " + id);
        }
        orderRepository.deleteById(id);
    }
}

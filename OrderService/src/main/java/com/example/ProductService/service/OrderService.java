package com.example.ProductService.service;

import com.example.ProductService.client.ProductClient;
import com.example.ProductService.event.OrderEvent;
import com.example.ProductService.entity.Order;
import com.example.ProductService.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.kafka.core.KafkaTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public OrderService(OrderRepository orderRepository,
            ProductClient productClient,
            KafkaTemplate<String, OrderEvent> kafkaTemplate) {
        this.orderRepository = orderRepository;
        this.productClient = productClient;
        this.kafkaTemplate = kafkaTemplate;
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
        Order saved = orderRepository.save(order);

        OrderEvent event = new OrderEvent(saved.getId(), saved.getProductId(), saved.getQuantity());
        kafkaTemplate.send("order-placed", event);

        log.info("Sipariş kaydedildi ve Kafka'ya event gönderildi. orderId={}", saved.getId());
        return saved;
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

    @CircuitBreaker(name = "productService", fallbackMethod = "urunFallback")
    public String siparisVer(Long productId) {
        productClient.getProductById(productId);
        return "Sipariş oluşturuldu, ürün onaylandı.";
    }


    public void deleteOrder(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new RuntimeException("Sipariş bulunamadı: " + id);
        }
        orderRepository.deleteById(id);
    }
}

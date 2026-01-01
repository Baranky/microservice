package com.example.ProductService.service;

import com.example.ProductService.client.ProductClient;
import com.example.ProductService.dto.OrderRequest;
import com.example.ProductService.entity.Order;
import com.example.ProductService.enums.OrderStatus;
import com.example.ProductService.event.OrderEvent;
import com.example.ProductService.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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


    @Transactional
    public Order createOrder(OrderRequest request) {
        log.info("Yeni sipariş oluşturuluyor: productId={}, quantity={}, customerEmail={}",
                request.productId(), request.quantity(), request.customerEmail());

        Order newOrder = new Order();
        newOrder.setProductId(request.productId());
        newOrder.setQuantity(request.quantity());
        newOrder.setTotalPrice(request.totalPrice());
        newOrder.setCustomerName(request.customerName());
        newOrder.setCustomerEmail(request.customerEmail());
        newOrder.setStatus(OrderStatus.PENDING);

        Order saved = orderRepository.save(newOrder);
        log.info("Sipariş kaydedildi: orderId={}, status=PENDING", saved.getId());

        OrderEvent event = new OrderEvent(saved.getId(), saved.getProductId(), saved.getQuantity(),
                saved.getTotalPrice(), saved.getCustomerEmail());
        kafkaTemplate.send("order-placed", event);

        log.info("Order-placed event gönderildi: orderId={}, productId={}, quantity={}",
                saved.getId(), saved.getProductId(), saved.getQuantity());
        return saved;
    }

    public Order updateOrder(Long id, OrderRequest request) {
        Order existingOrder = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sipariş bulunamadı: " + id));

        existingOrder.setProductId(request.productId());
        existingOrder.setQuantity(request.quantity());
        existingOrder.setTotalPrice(request.totalPrice());
        existingOrder.setCustomerName(request.customerName());
        existingOrder.setCustomerEmail(request.customerEmail());

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

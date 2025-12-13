package com.example.OrderService.controller;

import com.example.OrderService.client.ProductClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final ProductClient productClient;

    // Constructor Injection
    public OrderController(ProductClient productClient) {
        this.productClient = productClient;
    }

    @PostMapping
    public String createOrder(@RequestParam String productId) {
        // Sanki kendi metodumuzmuş gibi çağırıyoruz!
        // Arka planda HTTP isteği atılıyor.
        String productResponse = productClient.getProductById(productId);

        return "Sipariş oluşturuldu. Ürün Bilgisi: " + productResponse;
    }
}
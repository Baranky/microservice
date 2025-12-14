package com.example.OrderService.client;

import com.example.OrderService.dto.ProductDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// name = "product-service" -> Bu isim Eureka'daki kayıtlı isimle BİREBİR aynı olmalı!
@FeignClient(name = "product-service")
public interface ProductClient {

    // Karşı servisteki endpoint neyse aynısını buraya yazıyoruz.
    // Product Service'de: @GetMapping("/api/products/{id}") ise:
    @GetMapping("/api/products/{id}")
    ProductDTO getProductById(@PathVariable("id") Long id);
}

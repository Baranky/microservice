package com.example.ProductService.client;

import com.example.ProductService.dto.InventoryRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "inventory-service")
public interface InventoryClient {

    @PostMapping("/api/inventory")
    void createInventory(@RequestBody InventoryRequest request);

    @DeleteMapping("/api/inventory/product/{productId}")
    void deleteInventoryByProductId(@PathVariable("productId") Long productId);
}


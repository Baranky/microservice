package com.example.ProductService.controller;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@Slf4j
public class ProductController {

    @GetMapping("/{id}")
    ResponseEntity<?> getProductById(@PathVariable("id") String id){
        return ResponseEntity.ok(id+": urunlerde var");

    };
}

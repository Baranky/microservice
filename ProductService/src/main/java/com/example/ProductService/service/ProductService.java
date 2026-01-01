package com.example.ProductService.service;

import com.example.ProductService.client.InventoryClient;
import com.example.ProductService.dto.InventoryRequest;
import com.example.ProductService.dto.ProductRequest;
import com.example.ProductService.entity.Product;
import com.example.ProductService.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final InventoryClient inventoryClient;

    public ProductService(ProductRepository productRepository, InventoryClient inventoryClient) {
        this.productRepository = productRepository;
        this.inventoryClient = inventoryClient;
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    @Transactional
    public Product createProduct(ProductRequest request) {
        Product newProduct = new Product();
        newProduct.setName(request.name());
        newProduct.setDescription(request.description());
        newProduct.setPrice(request.price());
        Product savedProduct = productRepository.save(newProduct);

        try {
            InventoryRequest inventoryRequest = new InventoryRequest(savedProduct.getId(),request.stock());
            inventoryClient.createInventory(inventoryRequest);
        } catch (Exception e) {
            productRepository.delete(savedProduct);
            throw new RuntimeException("Ürün oluşturulamadı: " + e.getMessage());
        }

        return savedProduct;
    }

    public Product updateProduct(Long id, ProductRequest request) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ürün bulunamadı: " + id));

        existingProduct.setName(request.name());
        existingProduct.setDescription(request.description());
        existingProduct.setPrice(request.price());

        return productRepository.save(existingProduct);
    }


    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new RuntimeException("Ürün bulunamadı: " + id);
        }

        try {
            inventoryClient.deleteInventoryByProductId(id);
        } catch (Exception e) {
            log.warn("Inventory kaydı silinirken hata oluştu (devam ediliyor): productId={}", id, e);
        }

        productRepository.deleteById(id);
    }
}

package com.example.ProductService.dto;

public record InventoryRequest(
         Long productId,
         Integer stock
) {


}

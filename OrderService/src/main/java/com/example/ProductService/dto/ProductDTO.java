package com.example.ProductService.dto ;


import java.math.BigDecimal ;


public record ProductDTO (
          Long id ,
          String name ,
          String description ,
          BigDecimal price ,
          Integer stock 
){

    
}

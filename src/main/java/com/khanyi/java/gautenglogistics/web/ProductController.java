package com.khanyi.java.gautenglogistics.web;

import com.khanyi.java.gautenglogistics.repository.ProductRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** GET /products: everything the retailer sells, so the map page can offer it in an order. */
@RestController
@RequestMapping("/products")
class ProductController {

    private final ProductRepository productRepository;

    ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping
    List<ProductResponse> products() {
        return productRepository.findAll(Sort.by("name")).stream().map(ProductResponse::from).toList();
    }
}

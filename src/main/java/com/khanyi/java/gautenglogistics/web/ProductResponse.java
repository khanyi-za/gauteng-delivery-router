package com.khanyi.java.gautenglogistics.web;

import com.khanyi.java.gautenglogistics.domain.Product;

/** A product, as JSON. */
public record ProductResponse(String sku, String name) {

    static ProductResponse from(Product product) {
        return new ProductResponse(product.getSku(), product.getName());
    }
}

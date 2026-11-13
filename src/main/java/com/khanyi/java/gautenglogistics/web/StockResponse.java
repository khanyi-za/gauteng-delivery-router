package com.khanyi.java.gautenglogistics.web;

import com.khanyi.java.gautenglogistics.domain.StoreStock;

/** How many of one product a store has, as JSON. */
public record StockResponse(String sku, int quantity) {

    static StockResponse from(StoreStock stock) {
        return new StockResponse(stock.getSku(), stock.getQuantity());
    }
}

package com.khanyi.java.gautenglogistics.web;

import com.khanyi.java.gautenglogistics.repository.StoreRepository;
import com.khanyi.java.gautenglogistics.repository.StoreStockRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * GET /stores: every store, for showing them on the map. GET /stores/{id}/stock: how many of each
 * product one store has left.
 */
@RestController
@RequestMapping("/stores")
class StoreController {

    private final StoreRepository storeRepository;
    private final StoreStockRepository stockRepository;

    StoreController(StoreRepository storeRepository, StoreStockRepository stockRepository) {
        this.storeRepository = storeRepository;
        this.stockRepository = stockRepository;
    }

    @GetMapping
    List<StoreResponse> stores() {
        return storeRepository.findAll(Sort.by("name")).stream().map(StoreResponse::from).toList();
    }

    /** Answers 404 Not Found if there's no store with this id. */
    @GetMapping("/{id}/stock")
    ResponseEntity<List<StockResponse>> stock(@PathVariable long id) {
        if (!storeRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(stockRepository.findByStoreIdOrderBySku(id).stream().map(StockResponse::from).toList());
    }
}
package com.khanyi.java.gautenglogistics.web;

import com.khanyi.java.gautenglogistics.repository.StoreRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** GET /stores: every store, for showing them on the map. */
@RestController
@RequestMapping("/stores")
class StoreController {

    private final StoreRepository storeRepository;

    StoreController(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    @GetMapping
    List<StoreResponse> stores() {
        return storeRepository.findAll(Sort.by("name")).stream().map(StoreResponse::from).toList();
    }
}
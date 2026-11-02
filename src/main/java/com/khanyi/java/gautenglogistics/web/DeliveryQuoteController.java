package com.khanyi.java.gautenglogistics.web;

import com.khanyi.java.gautenglogistics.geo.Coordinate;
import com.khanyi.java.gautenglogistics.routing.StoreSelectionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * POST /delivery-quotes: which store would deliver this order, and how long it would take.
 * It doesn't place an order or change any stock.
 */
@RestController
@RequestMapping("/delivery-quotes")
class DeliveryQuoteController {

    private final StoreSelectionService storeSelectionService;

    DeliveryQuoteController(StoreSelectionService storeSelectionService) {
        this.storeSelectionService = storeSelectionService;
    }

    @PostMapping
    DeliveryQuoteResponse quote(@Valid @RequestBody DeliveryQuoteRequest request) {
        Coordinate customer = new Coordinate(request.lat(), request.lon());
        return DeliveryQuoteResponse.from(storeSelectionService.choose(customer, request.quantitiesBySku()));
    }
}

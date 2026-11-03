package com.khanyi.java.gautenglogistics.web;

import com.khanyi.java.gautenglogistics.domain.DeliveryOrder;
import com.khanyi.java.gautenglogistics.geo.Coordinate;
import com.khanyi.java.gautenglogistics.order.OrderService;
import com.khanyi.java.gautenglogistics.repository.DeliveryOrderRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * POST /orders places an order: the stock is taken from the chosen store straight away.
 * GET /orders/{id} shows an order.
 */
@RestController
@RequestMapping("/orders")
class OrderController {

    private final OrderService orderService;
    private final DeliveryOrderRepository orderRepository;

    OrderController(OrderService orderService, DeliveryOrderRepository orderRepository) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
    }

    /** Answers 201 Created, with a Location header saying where to find the new order. */
    @PostMapping
    ResponseEntity<OrderResponse> place(@Valid @RequestBody DeliveryQuoteRequest request) {
        DeliveryOrder order = orderService.place(new Coordinate(request.lat(), request.lon()), request.quantitiesBySku());
        return ResponseEntity.created(URI.create("/orders/" + order.getId())).body(OrderResponse.from(order));
    }

    /** Answers 404 Not Found if there's no order with this id. */
    @GetMapping("/{id}")
    ResponseEntity<OrderResponse> order(@PathVariable long id) {
        return ResponseEntity.of(orderRepository.findById(id).map(OrderResponse::from));
    }
}

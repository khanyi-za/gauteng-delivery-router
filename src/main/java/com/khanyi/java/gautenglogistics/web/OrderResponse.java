package com.khanyi.java.gautenglogistics.web;

import com.khanyi.java.gautenglogistics.domain.DeliveryOrder;
import com.khanyi.java.gautenglogistics.domain.OrderStatus;
import com.khanyi.java.gautenglogistics.geo.Coordinate;

import java.time.Instant;
import java.util.List;

import static com.khanyi.java.gautenglogistics.web.RouteResponse.round;

/**
 * An order, as JSON.
 *
 * @param driveMinutes how long the drive from the store to the customer takes
 */
public record OrderResponse(long id, StoreResponse store, Coordinate customer, OrderStatus status,
                            Instant placedAt, Instant deliverBy, double driveMinutes, List<Item> items) {

    public record Item(String sku, int quantity) {
    }

    static OrderResponse from(DeliveryOrder order) {
        return new OrderResponse(
                order.getId(),
                StoreResponse.from(order.getStore()),
                order.getCustomer(),
                order.getStatus(),
                order.getPlacedAt(),
                order.getDeliverBy(),
                round(order.getDriveTimeS() / 60, 1),
                order.getItems().stream().map(item -> new Item(item.sku(), item.quantity())).toList());
    }
}

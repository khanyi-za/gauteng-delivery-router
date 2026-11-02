package com.khanyi.java.gautenglogistics.routing;

import com.khanyi.java.gautenglogistics.domain.Store;
import com.khanyi.java.gautenglogistics.geo.Coordinate;
import com.khanyi.java.gautenglogistics.graph.Snap;

import java.util.List;

/**
 * The store chosen to deliver an order, and how the delivery gets to the customer.
 *
 * @param store      the store that will send the order
 * @param customer   the intersection the customer's address was matched to
 * @param route      the fastest route from the store to the customer
 * @param path       the shape of that route along the real roads
 * @param passedOver stores that are quicker to drive from but don't have everything, nearest first
 */
public record StoreChoice(Store store, Snap customer, Route route, List<Coordinate> path, List<Store> passedOver) {
}

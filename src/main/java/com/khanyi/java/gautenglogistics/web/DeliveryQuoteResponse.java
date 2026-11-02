package com.khanyi.java.gautenglogistics.web;

import com.khanyi.java.gautenglogistics.domain.Store;
import com.khanyi.java.gautenglogistics.geo.PolylineEncoder;
import com.khanyi.java.gautenglogistics.routing.StoreChoice;

import java.util.List;

import static com.khanyi.java.gautenglogistics.web.RouteResponse.round;

/**
 * What POST /delivery-quotes sends back, as JSON.
 *
 * @param store                the store that will deliver the order
 * @param distanceKm           how far the drive from the store to the customer is
 * @param durationMinutes      how long that drive takes
 * @param customer             where the delivery ends on the road map
 * @param passedOver           names of stores that are closer but don't have everything, nearest first
 * @param intersectionsVisited how many intersections the search looked at
 * @param geometry             the shape of the delivery route as an encoded polyline
 */
public record DeliveryQuoteResponse(StoreResponse store, double distanceKm, double durationMinutes,
                                    RouteResponse.SnappedPoint customer, List<String> passedOver,
                                    int intersectionsVisited, String geometry) {

    static DeliveryQuoteResponse from(StoreChoice choice) {
        return new DeliveryQuoteResponse(
                StoreResponse.from(choice.store()),
                round(choice.route().lengthM() / 1000, 2),
                round(choice.route().travelTimeS() / 60, 1),
                RouteResponse.SnappedPoint.from(choice.customer()),
                choice.passedOver().stream().map(Store::getName).toList(),
                choice.route().nodesVisited(),
                PolylineEncoder.encode(choice.path()));
    }
}

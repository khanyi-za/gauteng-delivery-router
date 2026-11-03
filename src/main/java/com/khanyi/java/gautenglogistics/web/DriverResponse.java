package com.khanyi.java.gautenglogistics.web;

import com.khanyi.java.gautenglogistics.dispatch.DriverPosition;
import com.khanyi.java.gautenglogistics.dispatch.DriverStatus;
import com.khanyi.java.gautenglogistics.domain.Driver;
import com.khanyi.java.gautenglogistics.geo.Coordinate;

import java.time.Instant;

import static com.khanyi.java.gautenglogistics.web.RouteResponse.round;

/**
 * A driver, as JSON.
 *
 * @param location where they are now, to 5 decimal places (about a metre)
 * @param orderId  the order they're taking, or have just delivered; null at the store
 * @param backAt   when they'll be back at the store; null if they're there
 */
public record DriverResponse(long id, String name, long storeId, DriverStatus status, Coordinate location,
                             Long orderId, Instant backAt) {

    static DriverResponse from(DriverPosition position) {
        Driver driver = position.driver();
        boolean out = position.order() != null;
        return new DriverResponse(
                driver.getId(),
                driver.getName(),
                driver.getStoreId(),
                position.status(),
                new Coordinate(round(position.location().lat(), 5), round(position.location().lon(), 5)),
                out ? position.order().getId() : null,
                out ? driver.getFreeAt() : null);
    }
}

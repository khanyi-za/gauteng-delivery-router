package com.khanyi.java.gautenglogistics.dispatch;

import com.khanyi.java.gautenglogistics.domain.DeliveryOrder;
import com.khanyi.java.gautenglogistics.domain.Driver;
import com.khanyi.java.gautenglogistics.geo.Coordinate;

/**
 * Where a driver is right now.
 *
 * @param order the order they're taking to the customer, or have just delivered on the way back;
 *              null while they're at the store
 */
public record DriverPosition(Driver driver, DriverStatus status, Coordinate location, DeliveryOrder order) {
}

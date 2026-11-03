package com.khanyi.java.gautenglogistics.dispatch;

import com.khanyi.java.gautenglogistics.domain.DeliveryOrder;
import com.khanyi.java.gautenglogistics.domain.Driver;
import com.khanyi.java.gautenglogistics.domain.Store;
import com.khanyi.java.gautenglogistics.geo.Coordinate;
import com.khanyi.java.gautenglogistics.repository.DeliveryOrderRepository;
import com.khanyi.java.gautenglogistics.repository.DriverRepository;
import com.khanyi.java.gautenglogistics.repository.StoreRepository;
import com.khanyi.java.gautenglogistics.routing.RouteService;
import com.khanyi.java.gautenglogistics.routing.TimedPath;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * Works out where each driver is right now, from the times dispatch saved: when they left the
 * store, when they reach the customer and when they're back. Nothing is saved while they drive.
 */
@Service
public class DriverTracker {

    private final DriverRepository driverRepository;
    private final DeliveryOrderRepository orderRepository;
    private final StoreRepository storeRepository;
    private final RouteService routeService;
    private final Clock clock;

    // The routes of trips still going, by order id, so each route is only searched for once.
    // Several requests can use it at the same time, so it's a map that's safe for that.
    private final Map<Long, Trip> trips = new ConcurrentHashMap<>();

    public DriverTracker(DriverRepository driverRepository, DeliveryOrderRepository orderRepository,
                         StoreRepository storeRepository, RouteService routeService, Clock clock) {
        this.driverRepository = driverRepository;
        this.orderRepository = orderRepository;
        this.storeRepository = storeRepository;
        this.routeService = routeService;
        this.clock = clock;
    }

    /** Every driver, by id. */
    public List<DriverPosition> positions() {
        Instant now = clock.instant();
        Map<Long, Store> stores = storeRepository.findAll().stream()
                .collect(Collectors.toMap(Store::getId, Function.identity()));
        List<Driver> drivers = driverRepository.findAll(Sort.by("id"));

        // The orders of the drivers who are still out, all in one query
        List<Long> outOrderIds = drivers.stream()
                .filter(driver -> driver.getLatestOrderId() != null && driver.getFreeAt().isAfter(now))
                .map(Driver::getLatestOrderId)
                .toList();
        Map<Long, DeliveryOrder> outOrders = orderRepository.findAllById(outOrderIds).stream()
                .collect(Collectors.toMap(DeliveryOrder::getId, Function.identity()));
        trips.keySet().retainAll(outOrders.keySet()); // forget the trips that have finished

        return drivers.stream()
                .map(driver -> position(driver, stores.get(driver.getStoreId()), outOrders.get(driver.getLatestOrderId()), now))
                .toList();
    }

    /** @param order the order they're out with, or null if they're at the store */
    private DriverPosition position(Driver driver, Store store, DeliveryOrder order, Instant now) {
        if (order == null) {
            return new DriverPosition(driver, DriverStatus.AT_STORE, store.getLocation(), null);
        }
        Trip trip = trips.computeIfAbsent(order.getId(), id -> new Trip(
                routeService.timedPath(store.getLocation(), order.getCustomer()),
                routeService.timedPath(order.getCustomer(), store.getLocation())));

        if (now.isBefore(order.getDeliveredAt())) {
            Coordinate location = along(trip.there(), order.getDispatchedAt(), order.getDeliveredAt(), now);
            return new DriverPosition(driver, DriverStatus.DELIVERING, location, order);
        }
        Coordinate location = along(trip.back(), order.getDeliveredAt(), driver.getFreeAt(), now);
        return new DriverPosition(driver, DriverStatus.RETURNING, location, order);
    }

    /**
     * Where on the path a driver is who leaves at one time and arrives at another. The path's
     * times are stretched to fit, because the saved times were rounded to whole seconds.
     */
    private static Coordinate along(TimedPath path, Instant leaves, Instant arrives, Instant now) {
        long driveMs = Duration.between(leaves, arrives).toMillis();
        double share = driveMs == 0 ? 1 : (double) Duration.between(leaves, now).toMillis() / driveMs;
        return path.positionAt(share * path.durationS());
    }

    /** The routes there and back for one order. */
    private record Trip(TimedPath there, TimedPath back) {
    }
}
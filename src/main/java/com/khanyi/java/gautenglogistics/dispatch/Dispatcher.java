package com.khanyi.java.gautenglogistics.dispatch;

import com.khanyi.java.gautenglogistics.domain.DeliveryOrder;
import com.khanyi.java.gautenglogistics.domain.Driver;
import com.khanyi.java.gautenglogistics.domain.OrderStatus;
import com.khanyi.java.gautenglogistics.graph.GridIndex;
import com.khanyi.java.gautenglogistics.graph.RoadGraph;
import com.khanyi.java.gautenglogistics.repository.DeliveryOrderRepository;
import com.khanyi.java.gautenglogistics.repository.DriverRepository;
import com.khanyi.java.gautenglogistics.routing.Dijkstra;
import com.khanyi.java.gautenglogistics.routing.Route;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Sends drivers out with orders. Each run hands out orders most urgent first (earliest
 * "deliver by" time), to whichever driver at the order's store is free. When every driver at a
 * store is out, its orders wait for a later run.
 */
@Service
public class Dispatcher {

    private static final Logger log = LoggerFactory.getLogger(Dispatcher.class);
    private static final ZoneId JOHANNESBURG = ZoneId.of("Africa/Johannesburg");

    /** Most urgent first. Orders due at the same time go in the order they were placed. */
    private static final Comparator<DeliveryOrder> MOST_URGENT_FIRST =
            Comparator.comparing(DeliveryOrder::getDeliverBy).thenComparing(DeliveryOrder::getId);

    /** The driver who's back at the store soonest first. */
    private static final Comparator<Driver> FREE_SOONEST_FIRST =
            Comparator.comparing(Driver::getFreeAt).thenComparing(Driver::getId);

    private final DeliveryOrderRepository orderRepository;
    private final DriverRepository driverRepository;
    private final RoadGraph graph;
    private final GridIndex gridIndex;
    private final Clock clock;

    public Dispatcher(DeliveryOrderRepository orderRepository, DriverRepository driverRepository,
                      RoadGraph graph, GridIndex gridIndex, Clock clock) {
        this.orderRepository = orderRepository;
        this.driverRepository = driverRepository;
        this.graph = graph;
        this.gridIndex = gridIndex;
        this.clock = clock;
    }
    /**
     * One dispatch run. All its changes are saved together when it ends: JPA notices the changed
     * orders and drivers, so there are no save calls.
     */
    @Transactional
    public void dispatch() {
        Instant now = clock.instant();


        for (DeliveryOrder order : orderRepository.findByStatus(OrderStatus.ON_THE_WAY)) {
            if (!order.getDeliveredAt().isAfter(now)) {
                order.markDelivered();
            }
        }

        PriorityQueue<DeliveryOrder> waiting = new PriorityQueue<>(MOST_URGENT_FIRST);
        waiting.addAll(orderRepository.findByStatus(OrderStatus.WAITING_FOR_DRIVER));
        if (waiting.isEmpty()) {
            return;
        }

        // Each store's drivers, the one who's back soonest at the front
        Map<Long, PriorityQueue<Driver>> driversByStore = new HashMap<>();
        for (Driver driver : driverRepository.findAll()) {
            driversByStore.computeIfAbsent(driver.getStoreId(), id -> new PriorityQueue<>(FREE_SOONEST_FIRST)).add(driver);
        }

        while (!waiting.isEmpty()) {
            DeliveryOrder order = waiting.poll();
            PriorityQueue<Driver> drivers = driversByStore.get(order.getStore().getId());
            if (drivers == null || drivers.peek().getFreeAt().isAfter(now)) {
                continue; // everyone at this store is out, even the one back soonest
            }
            Driver driver = drivers.poll();
            Instant arrives = now.plusSeconds(Math.round(order.getDriveTimeS()));
            order.dispatch(driver, now, arrives);
            driver.setFreeAt(arrives.plusSeconds(Math.round(driveBackS(order))));
            drivers.add(driver); // back in the queue, behind the drivers who'll be free sooner

            log.info("Order {} left {} with {}: arrives {}{}, driver back at {}", order.getId(),
                    order.getStore().getName(), driver.getName(), localTime(arrives),
                    order.isLate() ? " (LATE)" : "", localTime(driver.getFreeAt()));
        }
    }

    /**
     * How long the drive from the customer back to the store takes. It's worked out separately
     * because one-way streets can make it different from the drive there.
     */
    private double driveBackS(DeliveryOrder order) {
        long customer = gridIndex.nearest(order.getCustomer()).orElseThrow().node().id();
        long store = gridIndex.nearest(order.getStore().getLocation()).orElseThrow().node().id();
        return Dijkstra.fastestRoute(graph, customer, store).map(Route::travelTimeS).orElseThrow();
    }

    private static LocalTime localTime(Instant instant) {
        return instant.atZone(JOHANNESBURG).toLocalTime().truncatedTo(ChronoUnit.MINUTES);
    }
}

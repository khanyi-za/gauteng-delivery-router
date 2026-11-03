package com.khanyi.java.gautenglogistics.dispatch;

import com.khanyi.java.gautenglogistics.domain.DeliveryOrder;
import com.khanyi.java.gautenglogistics.domain.Driver;
import com.khanyi.java.gautenglogistics.domain.OrderItem;
import com.khanyi.java.gautenglogistics.domain.Store;
import com.khanyi.java.gautenglogistics.geo.Coordinate;
import com.khanyi.java.gautenglogistics.repository.DeliveryOrderRepository;
import com.khanyi.java.gautenglogistics.repository.DriverRepository;
import com.khanyi.java.gautenglogistics.repository.StoreRepository;
import com.khanyi.java.gautenglogistics.routing.RouteService;
import com.khanyi.java.gautenglogistics.routing.TimedPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * A store with a customer due east of it: the drive there takes 2 minutes and the drive back 4.
 * The routes come from a pretend RouteService, and the clock is fixed at noon.
 */
class DriverTrackerTest {

    private static final Coordinate STORE = new Coordinate(-26.10, 28.00);
    private static final Coordinate CUSTOMER = new Coordinate(-26.10, 28.02);
    private static final TimedPath THERE = new TimedPath(List.of(STORE, CUSTOMER), List.of(0.0, 120.0));
    private static final TimedPath BACK = new TimedPath(List.of(CUSTOMER, STORE), List.of(0.0, 240.0));

    private static final Instant NOON = Instant.parse("2026-11-02T10:00:00Z");

    private final DriverRepository driverRepository = mock(DriverRepository.class);
    private final DeliveryOrderRepository orderRepository = mock(DeliveryOrderRepository.class);
    private final StoreRepository storeRepository = mock(StoreRepository.class);
    private final RouteService routeService = mock(RouteService.class);
    private final DriverTracker tracker = new DriverTracker(driverRepository, orderRepository, storeRepository,
            routeService, Clock.fixed(NOON, ZoneOffset.UTC));

    private final Store store = store();

    @BeforeEach
    void setUp() {
        given(storeRepository.findAll()).willReturn(List.of(store));
        given(routeService.timedPath(STORE, CUSTOMER)).willReturn(THERE);
        given(routeService.timedPath(CUSTOMER, STORE)).willReturn(BACK);
    }

    @Test
    void aDriverWhoHasNeverLeftIsAtTheStore() {
        Driver driver = driver(NOON.minusSeconds(600));
        given(driverRepository.findAll(Sort.by("id"))).willReturn(List.of(driver));

        DriverPosition position = tracker.positions().getFirst();

        assertThat(position.status()).isEqualTo(DriverStatus.AT_STORE);
        assertThat(position.location()).isEqualTo(STORE);
        assertThat(position.order()).isNull();
    }

    @Test
    void aDriverOnTheWayIsPartWayToTheCustomer() {
        Driver driver = driver(NOON.plusSeconds(300));
        // Left a minute ago and arrives in a minute: halfway there
        DeliveryOrder order = orderOnTrip(driver, NOON.minusSeconds(60), NOON.plusSeconds(60));
        given(driverRepository.findAll(Sort.by("id"))).willReturn(List.of(driver));
        given(orderRepository.findAllById(List.of(5L))).willReturn(List.of(order));

        DriverPosition position = tracker.positions().getFirst();

        assertThat(position.status()).isEqualTo(DriverStatus.DELIVERING);
        assertThat(position.order()).isEqualTo(order);
        assertThat(position.location().lon()).isCloseTo(28.01, within(1e-9));
    }

    @Test
    void aDriverOnTheWayBackIsPartWayToTheStore() {
        Driver driver = driver(NOON.plusSeconds(180));
        // Delivered a minute ago and back in 3 minutes: a quarter of the way back
        DeliveryOrder order = orderOnTrip(driver, NOON.minusSeconds(180), NOON.minusSeconds(60));
        given(driverRepository.findAll(Sort.by("id"))).willReturn(List.of(driver));
        given(orderRepository.findAllById(List.of(5L))).willReturn(List.of(order));

        DriverPosition position = tracker.positions().getFirst();

        assertThat(position.status()).isEqualTo(DriverStatus.RETURNING);
        assertThat(position.order()).isEqualTo(order);
        assertThat(position.location().lon()).isCloseTo(28.015, within(1e-9));
    }

    @Test
    void aDriverBackFromTheirLatestTripIsAtTheStore() {
        Driver driver = driver(NOON.minusSeconds(1));
        orderOnTrip(driver, NOON.minusSeconds(400), NOON.minusSeconds(280));
        given(driverRepository.findAll(Sort.by("id"))).willReturn(List.of(driver));

        DriverPosition position = tracker.positions().getFirst();

        assertThat(position.status()).isEqualTo(DriverStatus.AT_STORE);
        assertThat(position.location()).isEqualTo(STORE);
        verify(orderRepository).findAllById(List.of()); // their old order isn't even read
        verifyNoInteractions(routeService);
    }

    @Test
    void eachTripsRoutesAreOnlySearchedForOnce() {
        Driver driver = driver(NOON.plusSeconds(300));
        DeliveryOrder order = orderOnTrip(driver, NOON.minusSeconds(60), NOON.plusSeconds(60));
        given(driverRepository.findAll(Sort.by("id"))).willReturn(List.of(driver));
        given(orderRepository.findAllById(List.of(5L))).willReturn(List.of(order));

        tracker.positions();
        tracker.positions();

        verify(routeService, times(1)).timedPath(STORE, CUSTOMER);
        verify(routeService, times(1)).timedPath(CUSTOMER, STORE);
    }

    /** Order 5, which the driver left the store with and takes to the customer at the given times. */
    private DeliveryOrder orderOnTrip(Driver driver, Instant leaves, Instant arrives) {
        DeliveryOrder order = new DeliveryOrder(store, CUSTOMER, List.of(new OrderItem("HAT", 1)),
                leaves.minusSeconds(600), leaves.plusSeconds(3_600), 120);
        ReflectionTestUtils.setField(order, "id", 5L);
        order.dispatch(driver, leaves, arrives);
        driver.startTrip(5L, driver.getFreeAt());
        return order;
    }

    private static Driver driver(Instant freeAt) {
        Driver driver = new Driver(1L, "Test driver", freeAt);
        ReflectionTestUtils.setField(driver, "id", 1L);
        return driver;
    }

    private static Store store() {
        Store store = new Store("Test store", STORE);
        ReflectionTestUtils.setField(store, "id", 1L);
        return store;
    }
}

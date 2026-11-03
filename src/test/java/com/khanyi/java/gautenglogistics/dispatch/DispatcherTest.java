package com.khanyi.java.gautenglogistics.dispatch;

import com.khanyi.java.gautenglogistics.domain.DeliveryOrder;
import com.khanyi.java.gautenglogistics.domain.Driver;
import com.khanyi.java.gautenglogistics.domain.OrderItem;
import com.khanyi.java.gautenglogistics.domain.OrderStatus;
import com.khanyi.java.gautenglogistics.domain.Store;
import com.khanyi.java.gautenglogistics.geo.Coordinate;
import com.khanyi.java.gautenglogistics.graph.Edge;
import com.khanyi.java.gautenglogistics.graph.GridIndex;
import com.khanyi.java.gautenglogistics.graph.RoadClass;
import com.khanyi.java.gautenglogistics.graph.RoadGraph;
import com.khanyi.java.gautenglogistics.graph.RoadNode;
import com.khanyi.java.gautenglogistics.repository.DeliveryOrderRepository;
import com.khanyi.java.gautenglogistics.repository.DriverRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * A one-way loop: the drive from the store to the customer takes 1 minute, but the drive back has
 * to go the long way round and takes 3.
 *
 * <pre>
 *   STORE ----1 min----> CUSTOMER
 *     ^                     |
 *     +--2 min-- BACK <-1 min-+
 * </pre>
 *
 * The database is replaced by pretend (mock) repositories, and the clock is fixed at noon.
 */
class DispatcherTest {

    private static final RoadNode STORE = new RoadNode(1, new Coordinate(-26.10, 28.00));
    private static final RoadNode CUSTOMER = new RoadNode(2, new Coordinate(-26.10, 28.01));
    private static final RoadNode BACK = new RoadNode(3, new Coordinate(-26.11, 28.005));
    private static final RoadGraph GRAPH = new RoadGraph(List.of(STORE, CUSTOMER, BACK), List.of(
            new Edge(12, 1, 2, RoadClass.PRIMARY, 1_000, 60),
            new Edge(23, 2, 3, RoadClass.PRIMARY, 1_000, 60),
            new Edge(31, 3, 1, RoadClass.PRIMARY, 2_000, 120)));

    private static final Instant NOON = Instant.parse("2026-11-02T10:00:00Z");

    private final DeliveryOrderRepository orderRepository = mock(DeliveryOrderRepository.class);
    private final DriverRepository driverRepository = mock(DriverRepository.class);
    private final Dispatcher dispatcher = new Dispatcher(orderRepository, driverRepository,
            GRAPH, new GridIndex(GRAPH, 2_000), Clock.fixed(NOON, ZoneOffset.UTC));

    private final Store store = store();

    @Test
    void aFreeDriverTakesTheOrder() {
        DeliveryOrder order = order(1, Duration.ofHours(3));
        Driver driver = driver(1, NOON.minusSeconds(600)); // back since 11:50
        given(orderRepository.findByStatus(OrderStatus.WAITING_FOR_DRIVER)).willReturn(List.of(order));
        given(driverRepository.findAll()).willReturn(List.of(driver));

        dispatcher.dispatch();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.ON_THE_WAY);
        assertThat(order.getDriver()).isEqualTo(driver);
        assertThat(order.getDispatchedAt()).isEqualTo(NOON);
        assertThat(order.getDeliveredAt()).isEqualTo(NOON.plusSeconds(60));
        // 1 minute there, then 3 minutes back the long way
        assertThat(driver.getFreeAt()).isEqualTo(NOON.plusSeconds(240));
        assertThat(driver.getLatestOrderId()).isEqualTo(1L);
        assertThat(order.isLate()).isFalse();
    }

    @Test
    void theMostUrgentOrderGoesFirst() {
        DeliveryOrder standard = order(1, Duration.ofHours(3)); // placed first, but due later
        DeliveryOrder express = order(2, Duration.ofHours(1));
        given(orderRepository.findByStatus(OrderStatus.WAITING_FOR_DRIVER)).willReturn(List.of(standard, express));
        given(driverRepository.findAll()).willReturn(List.of(driver(1, NOON)));

        dispatcher.dispatch();

        assertThat(express.getStatus()).isEqualTo(OrderStatus.ON_THE_WAY);
        assertThat(standard.getStatus()).isEqualTo(OrderStatus.WAITING_FOR_DRIVER); // the only driver is out
    }

    @Test
    void twoDriversTakeTheTwoMostUrgentOrders() {
        DeliveryOrder first = order(1, Duration.ofHours(1));
        DeliveryOrder second = order(2, Duration.ofHours(2));
        DeliveryOrder third = order(3, Duration.ofHours(3));
        Driver backLongAgo = driver(1, NOON.minusSeconds(3_600));
        Driver justBack = driver(2, NOON);
        given(orderRepository.findByStatus(OrderStatus.WAITING_FOR_DRIVER)).willReturn(List.of(third, second, first));
        given(driverRepository.findAll()).willReturn(List.of(justBack, backLongAgo));

        dispatcher.dispatch();

        assertThat(first.getDriver()).isEqualTo(backLongAgo); // the queue's front: free the soonest
        assertThat(second.getDriver()).isEqualTo(justBack);
        assertThat(third.getStatus()).isEqualTo(OrderStatus.WAITING_FOR_DRIVER);
    }

    @Test
    void anOrderWaitsWhileEveryDriverIsOut() {
        DeliveryOrder order = order(1, Duration.ofHours(3));
        given(orderRepository.findByStatus(OrderStatus.WAITING_FOR_DRIVER)).willReturn(List.of(order));
        given(driverRepository.findAll()).willReturn(List.of(driver(1, NOON.plusSeconds(300))));

        dispatcher.dispatch();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.WAITING_FOR_DRIVER);
        assertThat(order.getDriver()).isNull();
    }

    @Test
    void anOrderIsDeliveredOnceTheDriverHasArrived() {
        DeliveryOrder arrived = order(1, Duration.ofHours(3));
        arrived.dispatch(driver(1, NOON), NOON.minusSeconds(120), NOON.minusSeconds(60));
        DeliveryOrder stillDriving = order(2, Duration.ofHours(3));
        stillDriving.dispatch(driver(2, NOON), NOON.minusSeconds(30), NOON.plusSeconds(30));
        given(orderRepository.findByStatus(OrderStatus.ON_THE_WAY)).willReturn(List.of(arrived, stillDriving));

        dispatcher.dispatch();

        assertThat(arrived.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(stillDriving.getStatus()).isEqualTo(OrderStatus.ON_THE_WAY);
    }

    @Test
    void anOrderThatArrivesAfterItsDeadlineIsLate() {
        DeliveryOrder order = order(1, Duration.ofSeconds(30)); // due in 30 seconds, but the drive is 1 minute
        given(orderRepository.findByStatus(OrderStatus.WAITING_FOR_DRIVER)).willReturn(List.of(order));
        given(driverRepository.findAll()).willReturn(List.of(driver(1, NOON)));

        dispatcher.dispatch();

        assertThat(order.isLate()).isTrue();
    }

    /** An order placed at noon from the store to the customer, due within the given time. */
    private DeliveryOrder order(long id, Duration window) {
        DeliveryOrder order = new DeliveryOrder(store, CUSTOMER.location(), List.of(new OrderItem("HAT", 1)),
                NOON, NOON.plus(window), 60);
        ReflectionTestUtils.setField(order, "id", id);
        return order;
    }

    private static Driver driver(long id, Instant freeAt) {
        Driver driver = new Driver(1L, "Driver " + id, freeAt);
        ReflectionTestUtils.setField(driver, "id", id);
        return driver;
    }

    private static Store store() {
        Store store = new Store("Test store", STORE.location());
        ReflectionTestUtils.setField(store, "id", 1L);
        return store;
    }
}

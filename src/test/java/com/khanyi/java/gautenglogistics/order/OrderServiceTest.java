package com.khanyi.java.gautenglogistics.order;

import com.khanyi.java.gautenglogistics.domain.DeliveryOrder;
import com.khanyi.java.gautenglogistics.domain.OrderItem;
import com.khanyi.java.gautenglogistics.domain.OrderStatus;
import com.khanyi.java.gautenglogistics.domain.Store;
import com.khanyi.java.gautenglogistics.domain.StoreStock;
import com.khanyi.java.gautenglogistics.geo.Coordinate;
import com.khanyi.java.gautenglogistics.graph.RoadNode;
import com.khanyi.java.gautenglogistics.graph.Snap;
import com.khanyi.java.gautenglogistics.repository.DeliveryOrderRepository;
import com.khanyi.java.gautenglogistics.repository.StoreStockRepository;
import com.khanyi.java.gautenglogistics.routing.NoStoreHasStockException;
import com.khanyi.java.gautenglogistics.routing.Route;
import com.khanyi.java.gautenglogistics.routing.StoreChoice;
import com.khanyi.java.gautenglogistics.routing.StoreSelectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests the order steps and the trying again, with pretend (mock) repositories and a fixed clock.
 * The pretend transaction manager does nothing, so nothing here needs a database.
 */
class OrderServiceTest {

    private static final Instant NOON = Instant.parse("2026-11-02T10:00:00Z"); // 12:00 in Johannesburg
    private static final Coordinate CUSTOMER = new Coordinate(-26.1, 28.05);

    private final StoreSelectionService storeSelectionService = mock(StoreSelectionService.class);
    private final StoreStockRepository stockRepository = mock(StoreStockRepository.class);
    private final DeliveryOrderRepository orderRepository = mock(DeliveryOrderRepository.class);

    private final OrderService service = new OrderService(storeSelectionService, stockRepository, orderRepository,
            mock(PlatformTransactionManager.class), Clock.fixed(NOON, ZoneOffset.UTC));

    private final Store sandton = new Store("Sandton", new Coordinate(-26.1086, 28.0527));
    private final StoreStock hats = new StoreStock(1L, "HAT", 5);
    private final StoreStock bags = new StoreStock(1L, "BAG", 2);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(sandton, "id", 1L);
        given(storeSelectionService.choose(any(), any())).willReturn(new StoreChoice(sandton,
                new Snap(new RoadNode(9, CUSTOMER), 10), new Route(List.of(), 600, 5_000, 100), List.of(), List.of()));
        given(stockRepository.findByStoreIdAndSkuIn(eq(1L), any())).willAnswer(call -> {
            Collection<String> skus = call.getArgument(1);
            return Stream.of(hats, bags).filter(stock -> skus.contains(stock.getSku())).toList();
        });
        given(orderRepository.save(any())).willAnswer(call -> call.getArgument(0));
    }

    @Test
    void takesTheStockAndSavesTheOrder() {
        DeliveryOrder order = service.place(CUSTOMER, Map.of("HAT", 2, "BAG", 1), false);

        assertThat(hats.getQuantity()).isEqualTo(3);
        assertThat(bags.getQuantity()).isEqualTo(1);
        assertThat(order.getStore()).isEqualTo(sandton);
        assertThat(order.getCustomer()).isEqualTo(CUSTOMER);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.WAITING_FOR_DRIVER);
        assertThat(order.getPlacedAt()).isEqualTo(NOON);
        assertThat(order.getDeliverBy()).isEqualTo(Instant.parse("2026-11-02T13:00:00Z")); // 3 hours later
        assertThat(order.getDriveTimeS()).isEqualTo(600);
        assertThat(order.getItems()).containsExactly(new OrderItem("BAG", 1), new OrderItem("HAT", 2)); // sorted by code
    }

    @Test
    void anExpressOrderMustArriveWithinAnHour() {
        DeliveryOrder order = service.place(CUSTOMER, Map.of("HAT", 1), true);

        assertThat(order.getDeliverBy()).isEqualTo(Instant.parse("2026-11-02T11:00:00Z"));
    }

    @Test
    void triesAgainWhenAnotherOrderTookTheSameStockFirst() {
        given(orderRepository.save(any()))
                .willThrow(new ObjectOptimisticLockingFailureException(StoreStock.class, 1L))
                .willAnswer(call -> call.getArgument(0));

        DeliveryOrder order = service.place(CUSTOMER, Map.of("HAT", 1), false);

        assertThat(order.getStore()).isEqualTo(sandton);
        verify(storeSelectionService, times(2)).choose(CUSTOMER, Map.of("HAT", 1)); // chosen again with fresh stock
    }

    @Test
    void givesUpAfterThreeClashes() {
        given(orderRepository.save(any())).willThrow(new ObjectOptimisticLockingFailureException(StoreStock.class, 1L));

        assertThatThrownBy(() -> service.place(CUSTOMER, Map.of("HAT", 1), false))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
        verify(orderRepository, times(OrderService.MAX_ATTEMPTS)).save(any());
    }

    @Test
    void neverTakesMoreStockThanTheStoreHas() {
        assertThatThrownBy(() -> service.place(CUSTOMER, Map.of("BAG", 3), false))
                .isInstanceOf(NoStoreHasStockException.class);
        assertThat(bags.getQuantity()).isEqualTo(2);
        verify(orderRepository, never()).save(any());
    }
}

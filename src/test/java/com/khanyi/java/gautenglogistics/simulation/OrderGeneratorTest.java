package com.khanyi.java.gautenglogistics.simulation;

import com.khanyi.java.gautenglogistics.domain.DeliveryOrder;
import com.khanyi.java.gautenglogistics.domain.OrderItem;
import com.khanyi.java.gautenglogistics.domain.Product;
import com.khanyi.java.gautenglogistics.domain.Store;
import com.khanyi.java.gautenglogistics.geo.Coordinate;
import com.khanyi.java.gautenglogistics.geo.Haversine;
import com.khanyi.java.gautenglogistics.order.OrderService;
import com.khanyi.java.gautenglogistics.repository.ProductRepository;
import com.khanyi.java.gautenglogistics.repository.StoreRepository;
import com.khanyi.java.gautenglogistics.routing.NoStoreHasStockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * One store and three products, with a pretend OrderService. The random numbers come from a
 * fixed seed, so every run makes the same orders.
 */
class OrderGeneratorTest {

    private static final Coordinate SANDTON = new Coordinate(-26.1086, 28.0527);
    private static final int ORDERS = 500;

    private final OrderService orderService = mock(OrderService.class);
    private final StoreRepository storeRepository = mock(StoreRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final OrderGenerator generator = new OrderGenerator(orderService, storeRepository, productRepository,
            new OrderGeneratorProperties(true, Duration.ofSeconds(5), 8, 0.2), new Random(42));

    private final Store sandton = new Store("Sandton", SANDTON);

    @BeforeEach
    void setUp() {
        given(storeRepository.findAll()).willReturn(List.of(sandton));
        given(productRepository.findAll()).willReturn(List.of(
                new Product("HAT", "Hat"), new Product("SCARF", "Scarf"), new Product("BAG", "Bag")));
    }

    @Test
    void placesOrdersWithinTheDistanceOfAStore() {
        given(orderService.place(any(), any(), anyBoolean())).willReturn(anyOrder());

        for (int i = 0; i < ORDERS; i++) {
            generator.placeRandomOrder();
        }

        ArgumentCaptor<Coordinate> customers = ArgumentCaptor.captor();
        ArgumentCaptor<Map<String, Integer>> items = ArgumentCaptor.captor();
        ArgumentCaptor<Boolean> express = ArgumentCaptor.captor();
        verify(orderService, times(ORDERS)).place(customers.capture(), items.capture(), express.capture());

        assertThat(customers.getAllValues()).allSatisfy(customer ->
                assertThat(Haversine.distanceMetres(SANDTON, customer)).isLessThanOrEqualTo(8_000));
        // Spread out, not all bunched next to the store: about 3 in 4 are more than 4 km away
        assertThat(customers.getAllValues().stream().filter(c -> Haversine.distanceMetres(SANDTON, c) > 4_000))
                .hasSizeBetween(ORDERS * 65 / 100, ORDERS * 85 / 100);

        assertThat(items.getAllValues()).allSatisfy(order -> {
            assertThat(order).hasSizeBetween(1, 2);
            assertThat(order.keySet()).isSubsetOf("HAT", "SCARF", "BAG");
            assertThat(order.values()).containsOnly(1);
        });
        assertThat(items.getAllValues().stream().filter(order -> order.size() == 2))
                .hasSizeBetween(ORDERS * 20 / 100, ORDERS * 40 / 100); // about 30%
        assertThat(express.getAllValues().stream().filter(e -> e))
                .hasSizeBetween(ORDERS * 10 / 100, ORDERS * 30 / 100); // about 20%
    }

    @Test
    void anOrderNoStoreCanFillIsSkipped() {
        given(orderService.place(any(), any(), anyBoolean())).willThrow(new NoStoreHasStockException());

        assertThatCode(generator::placeRandomOrder).doesNotThrowAnyException();
    }

    @Test
    void withNoStoresNothingIsOrdered() {
        given(storeRepository.findAll()).willReturn(List.of());

        generator.placeRandomOrder();

        verifyNoInteractions(orderService);
    }

    @Test
    void aPointNorthIsTheRightDistanceAway() {
        Coordinate north = OrderGenerator.pointNear(SANDTON, 1_000, 0);

        assertThat(Haversine.distanceMetres(SANDTON, north)).isCloseTo(1_000, within(1.0));
        assertThat(north.lat()).isGreaterThan(SANDTON.lat());
        assertThat(north.lon()).isCloseTo(SANDTON.lon(), within(1e-9));
    }

    @Test
    void aPointEastIsTheRightDistanceAway() {
        Coordinate east = OrderGenerator.pointNear(SANDTON, 5_000, Math.PI / 2);

        assertThat(Haversine.distanceMetres(SANDTON, east)).isCloseTo(5_000, within(5.0));
        assertThat(east.lon()).isGreaterThan(SANDTON.lon());
        assertThat(east.lat()).isCloseTo(SANDTON.lat(), within(1e-9));
    }

    private DeliveryOrder anyOrder() {
        return new DeliveryOrder(sandton, SANDTON, List.of(new OrderItem("HAT", 1)),
                Instant.parse("2026-11-02T10:00:00Z"), Instant.parse("2026-11-02T13:00:00Z"), 600);
    }
}

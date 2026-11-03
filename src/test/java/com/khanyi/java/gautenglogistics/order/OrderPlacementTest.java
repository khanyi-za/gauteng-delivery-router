package com.khanyi.java.gautenglogistics.order;

import com.khanyi.java.gautenglogistics.TestcontainersConfiguration;
import com.khanyi.java.gautenglogistics.domain.DeliveryOrder;
import com.khanyi.java.gautenglogistics.domain.OrderItem;
import com.khanyi.java.gautenglogistics.domain.OrderStatus;
import com.khanyi.java.gautenglogistics.domain.Store;
import com.khanyi.java.gautenglogistics.domain.StoreStock;
import com.khanyi.java.gautenglogistics.geo.Coordinate;
import com.khanyi.java.gautenglogistics.graph.RoadNode;
import com.khanyi.java.gautenglogistics.graph.Snap;
import com.khanyi.java.gautenglogistics.repository.DeliveryOrderRepository;
import com.khanyi.java.gautenglogistics.repository.StoreRepository;
import com.khanyi.java.gautenglogistics.repository.StoreStockRepository;
import com.khanyi.java.gautenglogistics.routing.NoStoreHasStockException;
import com.khanyi.java.gautenglogistics.routing.Route;
import com.khanyi.java.gautenglogistics.routing.StoreChoice;
import com.khanyi.java.gautenglogistics.routing.StoreSelectionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Places orders against real Postgres, so the stock really changes in the database and the version
 * check really happens. Choosing the store is pretend: it always picks Soweto.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class OrderPlacementTest {

    private static final Coordinate CUSTOMER = new Coordinate(-26.25, 27.9);
    private static final String SCARF = "SCARF-SILK";

    @Autowired
    private OrderService orderService;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private StoreStockRepository stockRepository;

    @Autowired
    private DeliveryOrderRepository orderRepository;

    @MockitoBean
    private StoreSelectionService storeSelectionService;

    private Store soweto;

    @BeforeEach
    void setUp() {
        soweto = storeRepository.findAll().stream().filter(store -> store.getName().equals("Soweto")).findFirst().orElseThrow();
        given(storeSelectionService.choose(any(), any())).willAnswer(call -> sowetoChoice());
    }

    @AfterEach
    void putTheDemoDataBack() {
        orderRepository.deleteAll();
        setScarvesAtSoweto(5);
    }

    @Test
    void theOrderIsSavedAndTheStockIsTaken() {
        DeliveryOrder placed = orderService.place(CUSTOMER, Map.of(SCARF, 2));

        assertThat(scarvesAtSoweto()).isEqualTo(3);
        DeliveryOrder saved = orderRepository.findById(placed.getId()).orElseThrow();
        assertThat(saved.getStore().getName()).isEqualTo("Soweto");
        assertThat(saved.getCustomer()).isEqualTo(CUSTOMER);
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.WAITING_FOR_DRIVER);
        assertThat(saved.getItems()).containsExactly(new OrderItem(SCARF, 2));
        assertThat(Duration.between(saved.getPlacedAt(), saved.getDeliverBy())).isEqualTo(Duration.ofHours(3));
    }

    @Test
    void twoCustomersCantBothBuyTheLastScarf() throws Exception {
        setScarvesAtSoweto(1);
        // Make both orders read the stock (1 scarf left) before either of them saves, so they really clash
        CyclicBarrier bothHaveReadTheStock = new CyclicBarrier(2);
        AtomicInteger calls = new AtomicInteger();
        given(storeSelectionService.choose(any(), any())).willAnswer(call -> {
            stockRepository.findBySkuIn(List.of(SCARF));
            if (calls.incrementAndGet() <= 2) {
                bothHaveReadTheStock.await(10, TimeUnit.SECONDS);
            }
            return sowetoChoice();
        });

        CompletableFuture<Object> first = CompletableFuture.supplyAsync(this::placeScarfOrder);
        CompletableFuture<Object> second = CompletableFuture.supplyAsync(this::placeScarfOrder);
        List<Object> results = List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));

        // One order got the scarf. The other clashed, tried again, saw none left and was refused.
        assertThat(results).filteredOn(DeliveryOrder.class::isInstance).hasSize(1);
        assertThat(results).filteredOn(NoStoreHasStockException.class::isInstance).hasSize(1);
        assertThat(scarvesAtSoweto()).isZero();
        assertThat(orderRepository.count()).isEqualTo(1);
        verify(storeSelectionService, times(3)).choose(any(), any());
    }

    /** The order, or the exception if it failed. */
    private Object placeScarfOrder() {
        try {
            return orderService.place(CUSTOMER, Map.of(SCARF, 1));
        } catch (RuntimeException e) {
            return e;
        }
    }

    private StoreChoice sowetoChoice() {
        return new StoreChoice(soweto, new Snap(new RoadNode(1, CUSTOMER), 10),
                new Route(List.of(), 300, 2_000, 50), List.of(), List.of());
    }

    private int scarvesAtSoweto() {
        return sowetoScarfStock().getQuantity();
    }

    private StoreStock sowetoScarfStock() {
        return stockRepository.findByStoreIdAndSkuIn(soweto.getId(), List.of(SCARF)).getFirst();
    }

    private void setScarvesAtSoweto(int quantity) {
        StoreStock stock = sowetoScarfStock();
        stock.setQuantity(quantity);
        stockRepository.save(stock);
    }
}

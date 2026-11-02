package com.khanyi.java.gautenglogistics.routing;

import com.khanyi.java.gautenglogistics.domain.Product;
import com.khanyi.java.gautenglogistics.domain.Store;
import com.khanyi.java.gautenglogistics.domain.StoreStock;
import com.khanyi.java.gautenglogistics.geo.Coordinate;
import com.khanyi.java.gautenglogistics.geo.PolylineEncoder;
import com.khanyi.java.gautenglogistics.graph.Edge;
import com.khanyi.java.gautenglogistics.graph.EdgeGeometryLoader;
import com.khanyi.java.gautenglogistics.graph.GridIndex;
import com.khanyi.java.gautenglogistics.graph.RoadClass;
import com.khanyi.java.gautenglogistics.graph.RoadGraph;
import com.khanyi.java.gautenglogistics.graph.RoadNode;
import com.khanyi.java.gautenglogistics.repository.ProductRepository;
import com.khanyi.java.gautenglogistics.repository.StoreRepository;
import com.khanyi.java.gautenglogistics.repository.StoreStockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * A straight two-way road through three intersections, about 1 km apart, with a store at the
 * middle one and a store at the far end. The customer is at the other end.
 *
 * <pre>
 *   customer            NEAR store           FAR store
 *   WEST <------------> MIDDLE <-----------> EAST
 * </pre>
 *
 * The database is replaced by pretend (mock) repositories.
 */
class StoreSelectionServiceTest {

    private static final RoadNode WEST = new RoadNode(1, new Coordinate(-26.10, 28.00));
    private static final RoadNode MIDDLE = new RoadNode(2, new Coordinate(-26.10, 28.01));
    private static final RoadNode EAST = new RoadNode(3, new Coordinate(-26.10, 28.02));
    private static final List<Edge> EDGES = List.of(edge(12, 1, 2), edge(21, 2, 1), edge(23, 2, 3), edge(32, 3, 2));

    private static final Set<String> PRODUCTS_WE_SELL = Set.of("HAT", "BAG");

    private final RoadGraph graph = new RoadGraph(List.of(WEST, MIDDLE, EAST), EDGES);
    private final GridIndex gridIndex = new GridIndex(graph, 2_000);
    private final EdgeGeometryLoader geometryLoader = mock(EdgeGeometryLoader.class);
    private final StoreRepository storeRepository = mock(StoreRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final StoreStockRepository stockRepository = mock(StoreStockRepository.class);

    private final StoreSelectionService service = new StoreSelectionService(graph, gridIndex,
            new RouteService(graph, gridIndex, geometryLoader), storeRepository, productRepository, stockRepository);

    private final Store near = store(1L, "Near", MIDDLE);
    private final Store far = store(2L, "Far", EAST);

    @BeforeEach
    void setUp() {
        given(storeRepository.findAll()).willReturn(List.of(near, far));
        given(productRepository.findAllById(any())).willAnswer(call -> {
            Collection<String> skus = call.getArgument(0);
            return skus.stream().filter(PRODUCTS_WE_SELL::contains).map(sku -> new Product(sku, sku)).toList();
        });
        // Each road piece's shape is a straight line between its two ends
        Map<Long, Edge> edgesById = EDGES.stream().collect(Collectors.toMap(Edge::id, Function.identity()));
        given(geometryLoader.load(any())).willAnswer(call -> {
            Collection<Long> ids = call.getArgument(0);
            return ids.stream().collect(Collectors.toMap(id -> id, id -> PolylineEncoder.encode(List.of(
                    graph.node(edgesById.get(id).from()).location(), graph.node(edgesById.get(id).to()).location()))));
        });
    }

    @Test
    void theNearestStoreWithEverythingDelivers() {
        given(stockRepository.findBySkuIn(any())).willReturn(List.of(
                new StoreStock(1L, "HAT", 5), new StoreStock(2L, "HAT", 5)));

        StoreChoice choice = service.choose(WEST.location(), Map.of("HAT", 1));

        assertThat(choice.store()).isEqualTo(near);
        assertThat(choice.passedOver()).isEmpty();
        assertThat(choice.route().edges()).extracting(Edge::id).containsExactly(21L); // MIDDLE -> WEST
        assertThat(choice.path()).containsExactly(MIDDLE.location(), WEST.location());
    }

    @Test
    void aCloserStoreWithoutEnoughStockIsPassedOver() {
        // NEAR has only 1 bag, and the order wants 2
        given(stockRepository.findBySkuIn(any())).willReturn(List.of(
                new StoreStock(1L, "HAT", 5), new StoreStock(1L, "BAG", 1),
                new StoreStock(2L, "HAT", 5), new StoreStock(2L, "BAG", 5)));

        StoreChoice choice = service.choose(WEST.location(), Map.of("HAT", 1, "BAG", 2));

        assertThat(choice.store()).isEqualTo(far);
        assertThat(choice.passedOver()).containsExactly(near);
        assertThat(choice.route().edges()).extracting(Edge::id).containsExactly(32L, 21L); // EAST -> MIDDLE -> WEST
        assertThat(choice.customer().node()).isEqualTo(WEST);
    }

    @Test
    void aProductWeDontSellIsRefused() {
        assertThatThrownBy(() -> service.choose(WEST.location(), Map.of("HAT", 1, "BOAT", 1)))
                .isInstanceOf(UnknownProductException.class)
                .hasMessage("Unknown product code(s): BOAT");
    }

    @Test
    void whenNoStoreHasEverythingTheMapIsNotSearched() {
        // NEAR has hats but no bags, FAR has bags but no hats
        given(stockRepository.findBySkuIn(any())).willReturn(List.of(
                new StoreStock(1L, "HAT", 5), new StoreStock(1L, "BAG", 0),
                new StoreStock(2L, "HAT", 0), new StoreStock(2L, "BAG", 5)));

        assertThatThrownBy(() -> service.choose(WEST.location(), Map.of("HAT", 1, "BAG", 1)))
                .isInstanceOf(NoStoreHasStockException.class);
        verify(storeRepository, never()).findAll();
    }

    @Test
    void aCustomerFarFromAnyRoadIsRefused() {
        assertThatThrownBy(() -> service.choose(new Coordinate(-33.92, 18.42), Map.of("HAT", 1)))
                .isInstanceOf(NoRoadNearbyException.class);
    }

    private static Store store(Long id, String name, RoadNode at) {
        Store store = new Store(name, at.location());
        ReflectionTestUtils.setField(store, "id", id); // normally the database gives the id
        return store;
    }

    private static Edge edge(long id, long from, long to) {
        return new Edge(id, from, to, RoadClass.RESIDENTIAL, 1_000, 60);
    }
}

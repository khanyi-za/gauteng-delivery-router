package com.khanyi.java.gautenglogistics.routing;

import com.khanyi.java.gautenglogistics.domain.Store;
import com.khanyi.java.gautenglogistics.domain.StoreStock;
import com.khanyi.java.gautenglogistics.geo.Coordinate;
import com.khanyi.java.gautenglogistics.graph.GridIndex;
import com.khanyi.java.gautenglogistics.graph.RoadGraph;
import com.khanyi.java.gautenglogistics.graph.Snap;
import com.khanyi.java.gautenglogistics.repository.ProductRepository;
import com.khanyi.java.gautenglogistics.repository.StoreRepository;
import com.khanyi.java.gautenglogistics.repository.StoreStockRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Chooses which store should deliver an order: the one with the fastest drive to the customer
 * that has everything in the order. An order is never split between stores.
 */
@Service
public class StoreSelectionService {

    private final RoadGraph graph;
    private final GridIndex gridIndex;
    private final RouteService routeService;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final StoreStockRepository stockRepository;

    public StoreSelectionService(RoadGraph graph, GridIndex gridIndex, RouteService routeService,
                                 StoreRepository storeRepository, ProductRepository productRepository,
                                 StoreStockRepository stockRepository) {
        this.graph = graph;
        this.gridIndex = gridIndex;
        this.routeService = routeService;
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.stockRepository = stockRepository;
    }

    /**
     * @param customer where the order is going
     * @param items    how many of each product, by product code
     */
    public StoreChoice choose(Coordinate customer, Map<String, Integer> items) {
        Snap customerSnap = gridIndex.nearest(customer).orElseThrow(() -> new NoRoadNearbyException(customer));
        checkProductsExist(items.keySet());

        Set<Long> storesWithEverything = storesWithEverything(items);
        if (storesWithEverything.isEmpty()) {
            throw new NoStoreHasStockException(); // no need to search the map
        }

        // Which stores are at each intersection (a busy corner could have more than one)
        Map<Long, List<Store>> storesByNode = new HashMap<>();
        for (Store store : storeRepository.findAll()) {
            gridIndex.nearest(store.getLocation()).ifPresent(snap ->
                    storesByNode.computeIfAbsent(snap.node().id(), id -> new ArrayList<>()).add(store));
        }

        // Search backwards from the customer, so the first store reached is the quickest to drive
        // FROM. Stores reached on the way that are missing something are noted and passed over.
        List<Store> passedOver = new ArrayList<>();
        Route route = Dijkstra.fastestRouteFromNearest(graph, customerSnap.node().id(), node -> {
            for (Store store : storesByNode.getOrDefault(node, List.of())) {
                if (storesWithEverything.contains(store.getId())) {
                    return true;
                }
                passedOver.add(store);
            }
            return false;
        }).orElseThrow(() -> new IllegalStateException("No store can reach the customer"));

        long storeNode = route.edges().isEmpty() ? customerSnap.node().id() : route.edges().getFirst().from();
        Store chosen = storesByNode.get(storeNode).stream()
                .filter(store -> storesWithEverything.contains(store.getId()))
                .findFirst()
                .orElseThrow();
        return new StoreChoice(chosen, customerSnap, route,
                routeService.roadShape(graph.node(storeNode), route), passedOver);
    }

    private void checkProductsExist(Set<String> skus) {
        Set<String> unknown = new HashSet<>(skus);
        productRepository.findAllById(skus).forEach(product -> unknown.remove(product.getSku()));
        if (!unknown.isEmpty()) {
            throw new UnknownProductException(unknown.stream().sorted().toList());
        }
    }

    /** The ids of the stores that have enough of every product in the order. */
    private Set<Long> storesWithEverything(Map<String, Integer> items) {
        Map<Long, Integer> productsInStock = new HashMap<>(); // store id -> how many of the order's products it has enough of
        for (StoreStock stock : stockRepository.findBySkuIn(items.keySet())) {
            if (stock.getQuantity() >= items.get(stock.getSku())) {
                productsInStock.merge(stock.getStoreId(), 1, Integer::sum);
            }
        }
        Set<Long> stores = new HashSet<>();
        productsInStock.forEach((storeId, count) -> {
            if (count == items.size()) {
                stores.add(storeId);
            }
        });
        return stores;
    }
}

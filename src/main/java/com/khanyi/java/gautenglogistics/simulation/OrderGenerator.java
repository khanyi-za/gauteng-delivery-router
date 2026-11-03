package com.khanyi.java.gautenglogistics.simulation;

import com.khanyi.java.gautenglogistics.domain.DeliveryOrder;
import com.khanyi.java.gautenglogistics.domain.Product;
import com.khanyi.java.gautenglogistics.domain.Store;
import com.khanyi.java.gautenglogistics.geo.Coordinate;
import com.khanyi.java.gautenglogistics.order.OrderService;
import com.khanyi.java.gautenglogistics.repository.ProductRepository;
import com.khanyi.java.gautenglogistics.repository.StoreRepository;
import com.khanyi.java.gautenglogistics.routing.NoRoadNearbyException;
import com.khanyi.java.gautenglogistics.routing.NoStoreHasStockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

/**
 * Places made-up orders, the way customers would, so the live map has something to show without
 * anyone typing curl commands. Each customer is somewhere near a random store and wants one
 * product, or sometimes two.
 */
public class OrderGenerator {

    private static final Logger log = LoggerFactory.getLogger(OrderGenerator.class);

    /** How many metres one degree of latitude is (the earth's distance round, divided by 360). */
    private static final double METRES_PER_DEGREE = 111_195;

    /** How many orders want two different products instead of one. */
    private static final double TWO_PRODUCT_SHARE = 0.3;

    private final OrderService orderService;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final OrderGeneratorProperties properties;
    private final RandomGenerator random;

    OrderGenerator(OrderService orderService, StoreRepository storeRepository, ProductRepository productRepository,
                   OrderGeneratorProperties properties, RandomGenerator random) {
        this.orderService = orderService;
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.properties = properties;
        this.random = random;
    }

    @Scheduled(fixedDelayString = "${gautenglogistics.generator.interval}")
    void placeRandomOrder() {
        List<Store> stores = storeRepository.findAll();
        List<String> skus = productRepository.findAll().stream().map(Product::getSku).toList();
        if (stores.isEmpty() || skus.isEmpty()) {
            return; // nothing to order, or nowhere to order it from
        }
        Store nearby = stores.get(random.nextInt(stores.size()));
        // The square root spreads customers evenly over the circle round the store. Without it,
        // half of them would be in the middle quarter of it.
        double distanceM = properties.maxDistanceKm() * 1000 * Math.sqrt(random.nextDouble());
        Coordinate customer = pointNear(nearby.getLocation(), distanceM, random.nextDouble(2 * Math.PI));
        Map<String, Integer> items = randomItems(skus);
        boolean express = random.nextDouble() < properties.expressShare();

        try {
            DeliveryOrder order = orderService.place(customer, items, express);
            log.info("Random order {}{}: {} from {}", order.getId(), express ? " (express)" : "",
                    items.keySet(), order.getStore().getName());
        } catch (NoStoreHasStockException | NoRoadNearbyException e) {
            // A real customer would be told no. The generator just tries again next time.
            log.info("Random order not placed: {}", e.getMessage());
        }
    }

    /** One of a random product, or sometimes one each of two different products. */
    private Map<String, Integer> randomItems(List<String> skus) {
        List<String> shuffled = new ArrayList<>(skus);
        Collections.shuffle(shuffled, random);
        int count = random.nextDouble() < TWO_PRODUCT_SHARE ? 2 : 1;
        return shuffled.subList(0, Math.min(count, shuffled.size())).stream()
                .collect(Collectors.toMap(sku -> sku, sku -> 1));
    }

    /**
     * The point a distance away from another in a direction (0 is north, π/2 east). It treats the
     * ground as flat, which is close enough over a few kilometres.
     */
    static Coordinate pointNear(Coordinate from, double distanceM, double directionRadians) {
        double northM = distanceM * Math.cos(directionRadians);
        double eastM = distanceM * Math.sin(directionRadians);
        // Lines of longitude get closer together away from the equator, so a degree east is shorter
        double metresPerDegreeEast = METRES_PER_DEGREE * Math.cos(Math.toRadians(from.lat()));
        return new Coordinate(from.lat() + northM / METRES_PER_DEGREE, from.lon() + eastM / metresPerDegreeEast);
    }
}

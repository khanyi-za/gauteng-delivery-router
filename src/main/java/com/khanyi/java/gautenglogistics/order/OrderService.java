package com.khanyi.java.gautenglogistics.order;

import com.khanyi.java.gautenglogistics.domain.DeliveryOrder;
import com.khanyi.java.gautenglogistics.domain.OrderItem;
import com.khanyi.java.gautenglogistics.domain.StoreStock;
import com.khanyi.java.gautenglogistics.geo.Coordinate;
import com.khanyi.java.gautenglogistics.repository.DeliveryOrderRepository;
import com.khanyi.java.gautenglogistics.repository.StoreStockRepository;
import com.khanyi.java.gautenglogistics.routing.NoStoreHasStockException;
import com.khanyi.java.gautenglogistics.routing.StoreChoice;
import com.khanyi.java.gautenglogistics.routing.StoreSelectionService;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Places orders: chooses the store, takes the products out of its stock and saves the order,
 * all in one database transaction, so either all of it happens or none of it does.
 */
@Service
public class OrderService {

    /** Same-day delivery: every order must reach the customer within this long of being placed. */
    static final Duration DELIVERY_WINDOW = Duration.ofHours(3);

    /** How many times to try when other orders keep taking the same stock at the same moment. */
    static final int MAX_ATTEMPTS = 3;

    private final StoreSelectionService storeSelectionService;
    private final StoreStockRepository stockRepository;
    private final DeliveryOrderRepository orderRepository;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public OrderService(StoreSelectionService storeSelectionService, StoreStockRepository stockRepository,
                        DeliveryOrderRepository orderRepository, PlatformTransactionManager transactionManager,
                        Clock clock) {
        this.storeSelectionService = storeSelectionService;
        this.stockRepository = stockRepository;
        this.orderRepository = orderRepository;
        this.transaction = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    /**
     * @param customer where the order is going
     * @param items    how many of each product, by product code
     */
    public DeliveryOrder place(Coordinate customer, Map<String, Integer> items) {
        for (int attempt = 1; ; attempt++) {
            try {
                return transaction.execute(status -> placeOnce(customer, items));
            } catch (OptimisticLockingFailureException e) {
                // Another order changed the same stock between our reading it and saving it, so
                // nothing was saved. Start again with fresh numbers: the store we chose may not
                // have enough any more, and a different store may be chosen.
                if (attempt == MAX_ATTEMPTS) {
                    throw e;
                }
            }
        }
    }

    private DeliveryOrder placeOnce(Coordinate customer, Map<String, Integer> items) {
        StoreChoice choice = storeSelectionService.choose(customer, items);

        for (StoreStock stock : stockRepository.findByStoreIdAndSkuIn(choice.store().getId(), items.keySet())) {
            int left = stock.getQuantity() - items.get(stock.getSku());
            if (left < 0) {
                // The store was chosen because it had enough, so this shouldn't happen, but it's
                // cheap to make sure we never sell stock we don't have
                throw new NoStoreHasStockException();
            }
            stock.setQuantity(left);
        }
        // There's no save call for the stock: JPA notices the changed rows and updates them when the
        // transaction ends. Each update checks the row's version (see StoreStock), so if another
        // order changed the same row first, the update fails and the whole transaction is undone.

        Instant now = clock.instant();
        List<OrderItem> orderItems = items.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(item -> new OrderItem(item.getKey(), item.getValue()))
                .toList();
        return orderRepository.save(new DeliveryOrder(choice.store(), customer, orderItems,
                now, now.plus(DELIVERY_WINDOW), choice.route().travelTimeS()));
    }
}

package com.khanyi.java.gautenglogistics.repository;

import com.khanyi.java.gautenglogistics.TestcontainersConfiguration;
import com.khanyi.java.gautenglogistics.domain.Store;
import com.khanyi.java.gautenglogistics.domain.StoreStock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs against real Postgres with the Flyway migrations applied, so it also checks the demo data
 * and that the entities match the tables.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class StoreRepositoriesTest {

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StoreStockRepository stockRepository;

    @Test
    void theDemoDataIsLoaded() {
        assertThat(storeRepository.findAll()).extracting(Store::getName)
                .contains("Sandton", "Rosebank", "Menlyn", "Soweto").hasSize(8);
        assertThat(productRepository.count()).isEqualTo(8);
        assertThat(stockRepository.count()).isEqualTo(64); // every store has a row for every product
    }

    @Test
    void findsEveryStoresStockOfTheRequestedProducts() {
        List<StoreStock> sneakers = stockRepository.findBySkuIn(List.of("SNEAKER-WHT-8"));

        Map<String, Integer> byStore = sneakers.stream().collect(Collectors.toMap(
                stock -> storeRepository.findById(stock.getStoreId()).orElseThrow().getName(),
                StoreStock::getQuantity));
        assertThat(byStore).hasSize(8).containsEntry("Sandton", 0).containsEntry("Rosebank", 0)
                .containsEntry("Eastgate", 5);
    }

    @Test
    void twoChangesToTheSameStockAtOnceCantBothBeSaved() {
        Long rowId = stockRepository.findBySkuIn(List.of("HAT-BUCKET")).getFirst().getId();
        // Two separate reads of the same row, like two dispatch runs at the same time
        StoreStock first = stockRepository.findById(rowId).orElseThrow();
        StoreStock second = stockRepository.findById(rowId).orElseThrow();
        int original = first.getQuantity();

        first.setQuantity(original - 1);
        StoreStock saved = stockRepository.save(first);
        assertThat(saved.getVersion()).isEqualTo(second.getVersion() + 1);

        second.setQuantity(original - 1);
        assertThatThrownBy(() -> stockRepository.save(second))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        // Put the stock back so other tests see the demo data unchanged
        saved.setQuantity(original);
        stockRepository.save(saved);
    }
}

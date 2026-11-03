package com.khanyi.java.gautenglogistics.repository;

import com.khanyi.java.gautenglogistics.domain.StoreStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface StoreStockRepository extends JpaRepository<StoreStock, Long> {

    /** Every store's stock of these products. Spring Data writes the query from the method name. */
    List<StoreStock> findBySkuIn(Collection<String> skus);

    /** One store's stock of these products. */
    List<StoreStock> findByStoreIdAndSkuIn(Long storeId, Collection<String> skus);
}

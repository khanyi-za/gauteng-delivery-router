package com.khanyi.java.gautenglogistics.repository;


import com.khanyi.java.gautenglogistics.domain.DeliveryOrder;
import com.khanyi.java.gautenglogistics.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DeliveryOrderRepository extends JpaRepository<DeliveryOrder, Long> {

    List<DeliveryOrder> findByStatus(OrderStatus status);

    /** The latest time any order has recorded: when it's delivered, or else when it was placed. */
    @Query("select max(coalesce(o.deliveredAt, o.placedAt)) from DeliveryOrder o")
    Optional<Instant> findLatestTime();
}

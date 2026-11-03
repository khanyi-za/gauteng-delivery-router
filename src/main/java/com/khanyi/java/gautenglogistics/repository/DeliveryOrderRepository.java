package com.khanyi.java.gautenglogistics.repository;

import com.khanyi.java.gautenglogistics.domain.DeliveryOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryOrderRepository extends JpaRepository<DeliveryOrder, Long> {
}

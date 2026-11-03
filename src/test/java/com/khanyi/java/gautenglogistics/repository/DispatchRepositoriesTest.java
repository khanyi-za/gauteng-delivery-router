package com.khanyi.java.gautenglogistics.repository;

import com.khanyi.java.gautenglogistics.TestcontainersConfiguration;
import com.khanyi.java.gautenglogistics.domain.DeliveryOrder;
import com.khanyi.java.gautenglogistics.domain.Driver;
import com.khanyi.java.gautenglogistics.domain.OrderItem;
import com.khanyi.java.gautenglogistics.domain.OrderStatus;
import com.khanyi.java.gautenglogistics.domain.Store;
import com.khanyi.java.gautenglogistics.geo.Coordinate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs against real Postgres, so it checks V5/V6 and that drivers and dispatched orders are saved
 * and read back correctly.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class DispatchRepositoriesTest {

    // A day ahead, so the running dispatcher won't mark the test's order as delivered
    private static final Instant TOMORROW = Instant.now().plus(Duration.ofDays(1)).truncatedTo(ChronoUnit.SECONDS);

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private DeliveryOrderRepository orderRepository;

    @Autowired
    private StoreRepository storeRepository;

    @AfterEach
    void deleteTheTestOrders() {
        orderRepository.deleteAll();
    }

    @Test
    void everyStoreHasTwoDrivers() {
        List<Driver> drivers = driverRepository.findAll();

        assertThat(drivers).hasSize(16);
        Map<Long, Long> perStore = drivers.stream().collect(Collectors.groupingBy(Driver::getStoreId, Collectors.counting()));
        assertThat(perStore).hasSize(8).allSatisfy((store, count) -> assertThat(count).isEqualTo(2));
        assertThat(drivers).extracting(Driver::getName).contains("Sandton driver 1", "Sandton driver 2");
    }

    @Test
    void aDispatchedOrderIsSavedWithItsDriverAndTimes() {
        Store soweto = storeRepository.findAll().stream().filter(store -> store.getName().equals("Soweto")).findFirst().orElseThrow();
        Driver driver = driverRepository.findAll().stream().filter(d -> d.getName().equals("Soweto driver 1")).findFirst().orElseThrow();
        DeliveryOrder order = new DeliveryOrder(soweto, new Coordinate(-26.25, 27.9), List.of(new OrderItem("HAT-BUCKET", 1)),
                TOMORROW, TOMORROW.plus(Duration.ofHours(3)), 600);
        order.dispatch(driver, TOMORROW.plusSeconds(60), TOMORROW.plusSeconds(660));
        Long id = orderRepository.save(order).getId();

        DeliveryOrder saved = orderRepository.findById(id).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.ON_THE_WAY);
        assertThat(saved.getDriver().getName()).isEqualTo("Soweto driver 1");
        assertThat(saved.getDispatchedAt()).isEqualTo(TOMORROW.plusSeconds(60));
        assertThat(saved.getDeliveredAt()).isEqualTo(TOMORROW.plusSeconds(660));

        assertThat(orderRepository.findByStatus(OrderStatus.ON_THE_WAY)).extracting(DeliveryOrder::getId).containsExactly(id);
        assertThat(orderRepository.findByStatus(OrderStatus.WAITING_FOR_DRIVER)).isEmpty();
        assertThat(orderRepository.findLatestTime()).contains(TOMORROW.plusSeconds(660)); // the delivery time
    }
}

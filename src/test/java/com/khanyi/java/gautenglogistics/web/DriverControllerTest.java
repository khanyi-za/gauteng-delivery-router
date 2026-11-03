package com.khanyi.java.gautenglogistics.web;

import com.khanyi.java.gautenglogistics.dispatch.DriverPosition;
import com.khanyi.java.gautenglogistics.dispatch.DriverStatus;
import com.khanyi.java.gautenglogistics.dispatch.DriverTracker;
import com.khanyi.java.gautenglogistics.domain.DeliveryOrder;
import com.khanyi.java.gautenglogistics.domain.Driver;
import com.khanyi.java.gautenglogistics.domain.OrderItem;
import com.khanyi.java.gautenglogistics.domain.Store;
import com.khanyi.java.gautenglogistics.geo.Coordinate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/** Tests only the web layer, with a pretend DriverTracker. */
@WebMvcTest(DriverController.class)
class DriverControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private DriverTracker driverTracker;

    @Test
    void listsWhereEveryDriverIs() {
        Store soweto = new Store("Soweto", new Coordinate(-26.26, 27.892));
        Driver waiting = driver(1L, "Soweto driver 1", Instant.parse("2026-11-02T09:00:00Z"));
        Driver out = driver(2L, "Soweto driver 2", Instant.parse("2026-11-02T10:30:00Z"));
        DeliveryOrder order = new DeliveryOrder(soweto, new Coordinate(-26.25, 27.9), List.of(new OrderItem("HAT", 1)),
                Instant.parse("2026-11-02T09:55:00Z"), Instant.parse("2026-11-02T12:55:00Z"), 600);
        ReflectionTestUtils.setField(order, "id", 42L);
        given(driverTracker.positions()).willReturn(List.of(
                new DriverPosition(waiting, DriverStatus.AT_STORE, soweto.getLocation(), null),
                // Long decimals are rounded to 5 places
                new DriverPosition(out, DriverStatus.DELIVERING, new Coordinate(-26.2551234, 27.8961234), order)));

        assertThat(mvc.get().uri("/drivers"))
                .hasStatusOk()
                .bodyJson()
                .isStrictlyEqualTo("""
                        [
                          {"id": 1, "name": "Soweto driver 1", "storeId": 7, "status": "AT_STORE",
                           "location": {"lat": -26.26, "lon": 27.892}, "orderId": null, "backAt": null},
                          {"id": 2, "name": "Soweto driver 2", "storeId": 7, "status": "DELIVERING",
                           "location": {"lat": -26.25512, "lon": 27.89612}, "orderId": 42, "backAt": "2026-11-02T10:30:00Z"}
                        ]""");
    }

    private static Driver driver(long id, String name, Instant freeAt) {
        Driver driver = new Driver(7L, name, freeAt);
        ReflectionTestUtils.setField(driver, "id", id);
        return driver;
    }
}

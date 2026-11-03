package com.khanyi.java.gautenglogistics.web;

import com.khanyi.java.gautenglogistics.domain.DeliveryOrder;
import com.khanyi.java.gautenglogistics.domain.Driver;
import com.khanyi.java.gautenglogistics.domain.OrderItem;
import com.khanyi.java.gautenglogistics.domain.Store;
import com.khanyi.java.gautenglogistics.domain.StoreStock;
import com.khanyi.java.gautenglogistics.geo.Coordinate;
import com.khanyi.java.gautenglogistics.order.OrderService;
import com.khanyi.java.gautenglogistics.repository.DeliveryOrderRepository;
import com.khanyi.java.gautenglogistics.routing.NoStoreHasStockException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/** Tests only the web layer, with a pretend OrderService and repository. */
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    private static final String SCARF_ORDER = """
            {"lat": -26.25, "lon": 27.9, "items": [{"sku": "SCARF-SILK", "quantity": 2}]}""";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private DeliveryOrderRepository orderRepository;

    @Test
    void placingAnOrderAnswers201WithWhereToFindIt() {
        given(orderService.place(any(), any(), anyBoolean())).willReturn(scarfOrder());

        assertThat(post(SCARF_ORDER))
                .hasStatus(HttpStatus.CREATED)
                .hasHeader("Location", "/orders/42")
                .bodyJson()
                .isLenientlyEqualTo("""
                        {
                          "id": 42,
                          "store": {"id": 7, "name": "Soweto"},
                          "customer": {"lat": -26.25, "lon": 27.9},
                          "status": "WAITING_FOR_DRIVER",
                          "placedAt": "2026-11-02T10:00:00Z",
                          "deliverBy": "2026-11-02T13:00:00Z",
                          "driveMinutes": 12.5,
                          "items": [{"sku": "SCARF-SILK", "quantity": 2}],
                          "driver": null,
                          "dispatchedAt": null,
                          "deliveredAt": null,
                          "late": false
                        }""");
        verify(orderService).place(new Coordinate(-26.25, 27.9), Map.of("SCARF-SILK", 2), false);
    }

    @Test
    void anExpressOrderIsPassedOn() {
        given(orderService.place(any(), any(), anyBoolean())).willReturn(scarfOrder());

        post("""
                {"lat": -26.25, "lon": 27.9, "items": [{"sku": "SCARF-SILK", "quantity": 2}], "express": true}""")
                .exchange();

        verify(orderService).place(any(), any(), eq(true));
    }

    @Test
    void showsADispatchedOrder() {
        DeliveryOrder order = scarfOrder();
        Driver driver = new Driver(7L, "Soweto driver 1", Instant.parse("2026-11-02T10:30:00Z"));
        // Left at 10:05, arrives 10:17:30, after the deadline
        ReflectionTestUtils.setField(order, "deliverBy", Instant.parse("2026-11-02T10:15:00Z"));
        order.dispatch(driver, Instant.parse("2026-11-02T10:05:00Z"), Instant.parse("2026-11-02T10:17:30Z"));
        given(orderRepository.findById(42L)).willReturn(Optional.of(order));

        assertThat(mvc.get().uri("/orders/42"))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("""
                        {
                          "status": "ON_THE_WAY",
                          "driver": "Soweto driver 1",
                          "dispatchedAt": "2026-11-02T10:05:00Z",
                          "deliveredAt": "2026-11-02T10:17:30Z",
                          "late": true
                        }""");
    }

    @Test
    void listsOrdersNewestFirst() {
        given(orderRepository.findAll(any(Sort.class))).willReturn(List.of(scarfOrder()));

        assertThat(mvc.get().uri("/orders"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$[0].id").isEqualTo(42);
        verify(orderRepository).findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    @Test
    void showsAnOrder() {
        given(orderRepository.findById(42L)).willReturn(Optional.of(scarfOrder()));

        assertThat(mvc.get().uri("/orders/42"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.store.name").isEqualTo("Soweto");
    }

    @Test
    void anOrderThatDoesntExistIs404() {
        given(orderRepository.findById(99L)).willReturn(Optional.empty());

        assertThat(mvc.get().uri("/orders/99")).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void noStockIs422() {
        given(orderService.place(any(), any(), anyBoolean())).willThrow(new NoStoreHasStockException());

        assertThat(post(SCARF_ORDER)).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void stockThatKeepsChangingIs409() {
        given(orderService.place(any(), any(), anyBoolean()))
                .willThrow(new ObjectOptimisticLockingFailureException(StoreStock.class, 1L));

        assertThat(post(SCARF_ORDER))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("The stock changed while placing the order. Please try again.");
    }

    @Test
    void anOrderWithNoItemsIs400() {
        assertThat(post("""
                {"lat": -26.25, "lon": 27.9, "items": []}""")).hasStatus(HttpStatus.BAD_REQUEST);
    }

    private MockMvcTester.MockMvcRequestBuilder post(String json) {
        return mvc.post().uri("/orders").contentType(MediaType.APPLICATION_JSON).content(json);
    }

    private static DeliveryOrder scarfOrder() {
        Store soweto = new Store("Soweto", new Coordinate(-26.26, 27.892));
        ReflectionTestUtils.setField(soweto, "id", 7L);
        DeliveryOrder order = new DeliveryOrder(soweto, new Coordinate(-26.25, 27.9),
                List.of(new OrderItem("SCARF-SILK", 2)),
                Instant.parse("2026-11-02T10:00:00Z"), Instant.parse("2026-11-02T13:00:00Z"), 750);
        ReflectionTestUtils.setField(order, "id", 42L);
        return order;
    }
}

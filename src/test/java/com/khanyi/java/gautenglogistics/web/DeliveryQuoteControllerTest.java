package com.khanyi.java.gautenglogistics.web;

import com.khanyi.java.gautenglogistics.domain.Store;
import com.khanyi.java.gautenglogistics.geo.Coordinate;
import com.khanyi.java.gautenglogistics.graph.Edge;
import com.khanyi.java.gautenglogistics.graph.RoadClass;
import com.khanyi.java.gautenglogistics.graph.RoadNode;
import com.khanyi.java.gautenglogistics.graph.Snap;
import com.khanyi.java.gautenglogistics.routing.NoStoreHasStockException;
import com.khanyi.java.gautenglogistics.routing.Route;
import com.khanyi.java.gautenglogistics.routing.StoreChoice;
import com.khanyi.java.gautenglogistics.routing.StoreSelectionService;
import com.khanyi.java.gautenglogistics.routing.UnknownProductException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/** Tests only the web layer, with a pretend StoreSelectionService. */
@WebMvcTest(DeliveryQuoteController.class)
class DeliveryQuoteControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private StoreSelectionService storeSelectionService;

    @Test
    void returnsTheChosenStoreAndTheDeliveryRoute() {
        Store rosebank = new Store("Rosebank", new Coordinate(-26.146, 28.043));
        Store sandton = new Store("Sandton", new Coordinate(-26.1086, 28.0527));
        ReflectionTestUtils.setField(rosebank, "id", 2L);
        RoadNode customer = new RoadNode(9, new Coordinate(-26.1, 28.05));
        given(storeSelectionService.choose(any(), any())).willReturn(new StoreChoice(
                rosebank, new Snap(customer, 20),
                new Route(List.of(new Edge(1, 8, 9, RoadClass.PRIMARY, 6_543, 420)), 420, 6_543, 1_234),
                List.of(new Coordinate(-26.2, 28.0), new Coordinate(-26.1, 28.1)),
                List.of(sandton)));

        assertThat(mvc.post().uri("/delivery-quotes").contentType(MediaType.APPLICATION_JSON).content("""
                {"lat": -26.1, "lon": 28.05, "items": [{"sku": "SNEAKER-WHT-8", "quantity": 1}]}"""))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("""
                        {
                          "store": {"id": 2, "name": "Rosebank", "location": {"lat": -26.146, "lon": 28.043}},
                          "distanceKm": 6.54,
                          "durationMinutes": 7.0,
                          "customer": {"nodeId": 9, "snapDistanceM": 20.0},
                          "passedOver": ["Sandton"],
                          "intersectionsVisited": 1234,
                          "geometry": "~d|~C_w{iD_pR_pR"
                        }""");
    }

    @Test
    void theSameProductListedTwiceIsAddedUp() {
        given(storeSelectionService.choose(any(), any())).willThrow(new NoStoreHasStockException());

        mvc.post().uri("/delivery-quotes").contentType(MediaType.APPLICATION_JSON).content("""
                {"lat": -26.1, "lon": 28.05, "items": [{"sku": "HAT", "quantity": 1}, {"sku": "HAT", "quantity": 2}]}""")
                .exchange();

        verify(storeSelectionService).choose(eq(new Coordinate(-26.1, 28.05)), eq(Map.of("HAT", 3)));
    }

    @Test
    void noStoreWithEverythingIs422() {
        given(storeSelectionService.choose(any(), any())).willThrow(new NoStoreHasStockException());

        assertThat(post("""
                {"lat": -26.1, "lon": 28.05, "items": [{"sku": "HAT", "quantity": 99}]}"""))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("No store has everything in this order");
    }

    @Test
    void anUnknownProductIs422() {
        given(storeSelectionService.choose(any(), any())).willThrow(new UnknownProductException(List.of("BOAT")));

        assertThat(post("""
                {"lat": -26.1, "lon": 28.05, "items": [{"sku": "BOAT", "quantity": 1}]}"""))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("Unknown product code(s): BOAT");
    }

    @Test
    void anOrderWithNoItemsIs400() {
        assertThat(post("""
                {"lat": -26.1, "lon": 28.05, "items": []}""")).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void aQuantityOfZeroIs400() {
        assertThat(post("""
                {"lat": -26.1, "lon": 28.05, "items": [{"sku": "HAT", "quantity": 0}]}"""))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void aMissingLocationIs400() {
        assertThat(post("""
                {"items": [{"sku": "HAT", "quantity": 1}]}""")).hasStatus(HttpStatus.BAD_REQUEST);
    }

    private MockMvcTester.MockMvcRequestBuilder post(String json) {
        return mvc.post().uri("/delivery-quotes").contentType(MediaType.APPLICATION_JSON).content(json);
    }
}

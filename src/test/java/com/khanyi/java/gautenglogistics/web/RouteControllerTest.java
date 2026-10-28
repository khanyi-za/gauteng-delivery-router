package com.khanyi.java.gautenglogistics.web;

import com.khanyi.java.gautenglogistics.geo.Coordinate;
import com.khanyi.java.gautenglogistics.graph.Edge;
import com.khanyi.java.gautenglogistics.graph.RoadClass;
import com.khanyi.java.gautenglogistics.graph.RoadNode;
import com.khanyi.java.gautenglogistics.graph.Snap;
import com.khanyi.java.gautenglogistics.routing.NoRoadNearbyException;
import com.khanyi.java.gautenglogistics.routing.Route;
import com.khanyi.java.gautenglogistics.routing.RoutePlan;
import com.khanyi.java.gautenglogistics.routing.RouteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * Tests only the web layer: the real controller, with a pretend RouteService, so no map or
 * database is needed. It checks the URL, the parameters, the JSON and the error codes.
 */
@WebMvcTest(RouteController.class)
class RouteControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private RouteService routeService;

    @Test
    void returnsTheRouteAsJson() {
        RoadNode start = new RoadNode(1, new Coordinate(-26.2, 28.0));
        RoadNode end = new RoadNode(2, new Coordinate(-26.1, 28.1));
        Edge road = new Edge(7, 1, 2, RoadClass.PRIMARY, 12_345, 600);
        given(routeService.plan(any(), any())).willReturn(new RoutePlan(
                new Snap(start, 12.34), new Snap(end, 5),
                new Route(List.of(road), 600, 12_345, 99),
                List.of(start.location(), end.location())));

        // "geometry" is the path (-26.2, 28.0) -> (-26.1, 28.1) written as an encoded polyline
        assertThat(mvc.get().uri("/routes?fromLat=-26.2&fromLon=28.0&toLat=-26.1&toLon=28.1"))
                .hasStatusOk()
                .bodyJson()
                .isLenientlyEqualTo("""
                        {
                          "distanceKm": 12.35,
                          "durationMinutes": 10.0,
                          "start": {"nodeId": 1, "location": {"lat": -26.2, "lon": 28.0}, "snapDistanceM": 12.3},
                          "end": {"nodeId": 2, "snapDistanceM": 5.0},
                          "roadSegments": 1,
                          "intersectionsVisited": 99,
                          "geometry": "~d|~C_w{iD_pR_pR"
                        }""");
    }

    @Test
    void aPointFarFromAnyRoadIs422() {
        given(routeService.plan(any(), any())).willThrow(new NoRoadNearbyException(new Coordinate(-33.9, 18.4)));

        assertThat(mvc.get().uri("/routes?fromLat=-33.9&fromLon=18.4&toLat=-26.1&toLon=28.1"))
                .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                .bodyJson()
                .extractingPath("$.detail").isEqualTo("No road found near -33.9, 18.4");
    }

    @Test
    void aLatitudeOutsideMinus90To90Is400() {
        assertThat(mvc.get().uri("/routes?fromLat=-126&fromLon=28.0&toLat=-26.1&toLon=28.1"))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void aMissingParameterIs400() {
        assertThat(mvc.get().uri("/routes?fromLat=-26.2"))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }
}

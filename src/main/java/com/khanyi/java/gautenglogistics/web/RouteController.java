package com.khanyi.java.gautenglogistics.web;

import com.khanyi.java.gautenglogistics.geo.Coordinate;
import com.khanyi.java.gautenglogistics.routing.RouteService;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The route API. Example:
 * GET /routes?fromLat=-26.2041&fromLon=28.0473&toLat=-25.7479&toLon=28.2293
 */
@RestController
@RequestMapping("/routes")
class RouteController {

    private final RouteService routeService;

    RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping
    RouteResponse route(@RequestParam @DecimalMin("-90") @DecimalMax("90") double fromLat,
                        @RequestParam @DecimalMin("-180") @DecimalMax("180") double fromLon,
                        @RequestParam @DecimalMin("-90") @DecimalMax("90") double toLat,
                        @RequestParam @DecimalMin("-180") @DecimalMax("180") double toLon) {
        return RouteResponse.from(routeService.plan(new Coordinate(fromLat, fromLon), new Coordinate(toLat, toLon)));
    }
}

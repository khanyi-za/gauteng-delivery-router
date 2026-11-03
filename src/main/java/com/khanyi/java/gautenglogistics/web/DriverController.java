package com.khanyi.java.gautenglogistics.web;

import com.khanyi.java.gautenglogistics.dispatch.DriverTracker;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** GET /drivers: where every driver is right now, for the live map. */
@RestController
@RequestMapping("/drivers")
class DriverController {

    private final DriverTracker driverTracker;

    DriverController(DriverTracker driverTracker) {
        this.driverTracker = driverTracker;
    }

    @GetMapping
    List<DriverResponse> drivers() {
        return driverTracker.positions().stream().map(DriverResponse::from).toList();
    }
}

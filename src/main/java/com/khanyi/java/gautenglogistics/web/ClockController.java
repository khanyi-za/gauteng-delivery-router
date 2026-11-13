package com.khanyi.java.gautenglogistics.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;

/**
 * GET /clock: the simulated time, so the map page can show it next to order deadlines, which are
 * in simulated time too.
 */
@RestController
@RequestMapping("/clock")
class ClockController {

    private final Clock clock;
    private final double speed;

    ClockController(Clock clock, @Value("${gautenglogistics.simulation.speed}") double speed) {
        this.clock = clock;
        this.speed = speed;
    }

    @GetMapping
    ClockResponse clock() {
        return new ClockResponse(clock.instant(), speed);
    }
}

package com.khanyi.java.gautenglogistics.web;

import java.time.Instant;

/**
 * What GET /clock sends back, as JSON.
 *
 * @param now   the simulated time now
 * @param speed how many times faster than real time the simulated clock runs
 */
public record ClockResponse(Instant now, double speed) {
}

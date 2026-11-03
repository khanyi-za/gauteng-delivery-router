package com.khanyi.java.gautenglogistics;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * A clock that runs faster than real time, so a day of deliveries can be watched in minutes.
 * At a speed of 60, one real second is one simulated minute.
 */
public class SimulatedClock extends Clock {

    private final Clock realClock;
    private final Instant realStart;
    private final Instant simulatedStart;
    private final double speed;

    /**
     * @param realClock      the real clock it's driven by
     * @param simulatedStart the simulated time to start from
     * @param speed          how many times faster than real time it runs
     */
    public SimulatedClock(Clock realClock, Instant simulatedStart, double speed) {
        this.realClock = realClock;
        this.realStart = realClock.instant();
        this.simulatedStart = simulatedStart;
        this.speed = speed;
    }

    @Override
    public Instant instant() {
        Duration realTimePassed = Duration.between(realStart, realClock.instant());
        return simulatedStart.plusNanos(Math.round(realTimePassed.toNanos() * speed));
    }

    /** Always UTC. Times are only turned into Johannesburg time for showing to people. */
    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        throw new UnsupportedOperationException("The simulated clock is always UTC");
    }
}

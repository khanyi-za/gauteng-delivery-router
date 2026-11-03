package com.khanyi.java.gautenglogistics;

import com.khanyi.java.gautenglogistics.repository.DeliveryOrderRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Instant;

/**
 * The clock the app reads the time from: a simulated clock that runs faster than real time.
 * Asking this bean instead of calling Instant.now() also lets tests use a fixed time.
 */
@Configuration(proxyBeanMethods = false)
class ClockConfiguration {

    /**
     * Starts at the real time, or later if an earlier run of the app has already simulated past it,
     * so simulated time never goes backwards between restarts.
     */
    @Bean
    Clock clock(DeliveryOrderRepository orderRepository,
                @Value("${gautenglogistics.simulation.speed}") double speed) {
        Clock realClock = Clock.systemUTC();
        Instant start = orderRepository.findLatestTime()
                .filter(latest -> latest.isAfter(realClock.instant()))
                .orElse(realClock.instant());
        return new SimulatedClock(realClock, start, speed);
    }
}

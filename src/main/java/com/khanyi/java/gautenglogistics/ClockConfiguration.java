package com.khanyi.java.gautenglogistics;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * The clock the app reads the time from. Asking this bean instead of calling Instant.now() lets tests
 * use a fixed time, and later lets the delivery simulation run faster than real time.
 */
@Configuration(proxyBeanMethods = false)
class ClockConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}

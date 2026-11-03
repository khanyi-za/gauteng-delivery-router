package com.khanyi.java.gautenglogistics.dispatch;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.concurrent.TimeUnit;

/**
 * Runs the dispatcher every second of real time (a minute of simulated time, at speed 60).
 * Switched off with gautenglogistics.dispatch.enabled=false, which some tests do.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnBooleanProperty(name = "gautenglogistics.dispatch.enabled", matchIfMissing = true)
class DispatchScheduler {

    private final Dispatcher dispatcher;

    DispatchScheduler(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    // fixedDelay: the next run starts a second after the last one finished, so runs never overlap
    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.SECONDS)
    void run() {
        dispatcher.dispatch();
    }
}

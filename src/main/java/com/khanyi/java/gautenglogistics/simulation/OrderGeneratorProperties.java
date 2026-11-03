package com.khanyi.java.gautenglogistics.simulation;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Settings under gautenglogistics.generator in application.properties.
 *
 * @param enabled       whether to place random orders at all
 * @param interval      how long to wait between random orders, in real time
 * @param maxDistanceKm how far from a store a random customer can be
 * @param expressShare  how many of the random orders are express, from 0 (none) to 1 (all)
 */
@ConfigurationProperties("gautenglogistics.generator")
public record OrderGeneratorProperties(boolean enabled, Duration interval, double maxDistanceKm, double expressShare) {
}

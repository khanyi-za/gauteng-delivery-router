package com.khanyi.java.gautenglogistics.graph;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The OSM highway types a delivery vehicle can drive on, with the speed to assume
 * when a road has no maxspeed tag. Defaults follow South African limits:
 * 120 km/h on freeways, 100 km/h rural, 60 km/h urban.
 */
public enum RoadClass {

    MOTORWAY("motorway", 120, false),
    MOTORWAY_LINK("motorway_link", 60, false),
    TRUNK("trunk", 100, false),
    TRUNK_LINK("trunk_link", 60, false),
    PRIMARY("primary", 80, true),
    PRIMARY_LINK("primary_link", 60, true),
    SECONDARY("secondary", 70, true),
    SECONDARY_LINK("secondary_link", 60, true),
    TERTIARY("tertiary", 60, true),
    TERTIARY_LINK("tertiary_link", 50, true),
    UNCLASSIFIED("unclassified", 60, true),
    RESIDENTIAL("residential", 60, true),
    LIVING_STREET("living_street", 20, true),
    SERVICE("service", 30, true);

    /**
     * Fastest speed any edge may have. A* relies on this to never overestimate travel time.
     */
    public static final double MAX_SPEED_KMH = 120;

    private static final Map<String, RoadClass> BY_TAG = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(RoadClass::tag, Function.identity()));

    private final String tag;
    private final double defaultSpeedKmh;
    private final boolean snappable;

    RoadClass(String tag, double defaultSpeedKmh, boolean snappable) {
        this.tag = tag;
        this.defaultSpeedKmh = defaultSpeedKmh;
        this.snappable = snappable;
    }

    public static Optional<RoadClass> fromTag(String highway) {
        return Optional.ofNullable(BY_TAG.get(highway));
    }

    /**
     * The OSM highway tag value, e.g. "residential".
     */
    public String tag() {
        return tag;
    }

    public double defaultSpeedKmh() {
        return defaultSpeedKmh;
    }

    /**
     * Whether stores and customers may snap to this road. False for motorways, trunk roads and their ramps.
     */
    public boolean snappable() {
        return snappable;
    }
}

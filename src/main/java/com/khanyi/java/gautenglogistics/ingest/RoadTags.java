package com.khanyi.java.gautenglogistics.ingest;

import com.khanyi.java.gautenglogistics.graph.RoadClass;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;

/** Interprets the OSM tags on a way: is it drivable, which way does it run, and how fast. */
final class RoadTags {

    private static final Set<String> NO_ACCESS = Set.of("no", "private");

    /** Service roads that are part of a car park or property rather than the road network. */
    private static final Set<String> EXCLUDED_SERVICE = Set.of(
            "parking_aisle", "driveway", "drive-through", "emergency_access");

    private static final double KMH_PER_MPH = 1.609344;

    private RoadTags() {
    }

    /** The road class if a delivery vehicle may drive on this way, otherwise empty. */
    static Optional<RoadClass> roadClass(Map<String, String> tags) {
        String highway = tags.get("highway");
        if (highway == null || "yes".equals(tags.get("area"))) {
            return Optional.empty();
        }
        // getOrDefault because Set.of(...).contains(null) throws, and most roads have none of these tags
        if (NO_ACCESS.contains(tags.getOrDefault("access", ""))
                || NO_ACCESS.contains(tags.getOrDefault("motor_vehicle", ""))
                || NO_ACCESS.contains(tags.getOrDefault("motorcar", ""))) {
            return Optional.empty();
        }
        if ("service".equals(highway) && EXCLUDED_SERVICE.contains(tags.getOrDefault("service", ""))) {
            return Optional.empty();
        }
        return RoadClass.fromTag(highway);
    }

    /**
     * The direction of travel, or empty for roads whose direction changes during the day
     * (oneway=reversible or alternating), which can't be routed on safely.
     */
    static Optional<Direction> direction(Map<String, String> tags, RoadClass roadClass) {
        String oneway = tags.get("oneway");
        if (oneway != null) {
            return switch (oneway) {
                case "yes", "true", "1" -> Optional.of(Direction.FORWARD);
                case "-1", "reverse" -> Optional.of(Direction.BACKWARD);
                case "reversible", "alternating" -> Optional.empty();
                default -> Optional.of(Direction.TWO_WAY);
            };
        }
        String junction = tags.get("junction");
        if (roadClass == RoadClass.MOTORWAY || "roundabout".equals(junction) || "circular".equals(junction)) {
            return Optional.of(Direction.FORWARD); // one-way by convention, even without the tag
        }
        return Optional.of(Direction.TWO_WAY);
    }

    /** The speed to route with: the maxspeed tag if it can be read, else the road class default. */
    static double speedKmh(Map<String, String> tags, RoadClass roadClass) {
        double speed = parseMaxSpeedKmh(tags.get("maxspeed")).orElse(roadClass.defaultSpeedKmh());
        return Math.min(speed, RoadClass.MAX_SPEED_KMH);
    }

    /**
     * Reads a maxspeed value such as "60", "50 mph", "ZA:urban" or "60;80".
     * Multiple values give the lowest. "none" and anything unreadable give empty.
     */
    static OptionalDouble parseMaxSpeedKmh(String raw) {
        if (raw == null || raw.isBlank()) {
            return OptionalDouble.empty();
        }
        double lowest = Double.MAX_VALUE;
        for (String part : raw.split(";")) {
            OptionalDouble speed = parseSingleSpeedKmh(part.trim().toLowerCase(Locale.ROOT));
            if (speed.isPresent()) {
                lowest = Math.min(lowest, speed.getAsDouble());
            }
        }
        return lowest == Double.MAX_VALUE ? OptionalDouble.empty() : OptionalDouble.of(lowest);
    }

    private static OptionalDouble parseSingleSpeedKmh(String value) {
        switch (value) {
            case "za:urban":
                return OptionalDouble.of(60);
            case "za:rural":
                return OptionalDouble.of(100);
            case "za:motorway":
                return OptionalDouble.of(120);
            default:
                break;
        }
        boolean mph = value.endsWith("mph");
        String number = value.replace("mph", "").replace("km/h", "").replace("kmh", "").trim();
        try {
            double speed = Double.parseDouble(number);
            if (speed <= 0) {
                return OptionalDouble.empty();
            }
            return OptionalDouble.of(mph ? speed * KMH_PER_MPH : speed);
        } catch (NumberFormatException e) {
            return OptionalDouble.empty();
        }
    }
}

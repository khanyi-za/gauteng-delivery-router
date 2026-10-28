package com.khanyi.java.gautenglogistics.web;


import com.khanyi.java.gautenglogistics.geo.Coordinate;
import com.khanyi.java.gautenglogistics.geo.PolylineEncoder;
import com.khanyi.java.gautenglogistics.graph.Snap;
import com.khanyi.java.gautenglogistics.routing.RoutePlan;

/**
 * What GET /routes sends back, as JSON.
 *
 * @param distanceKm           how far the route is
 * @param durationMinutes      how long it takes to drive
 * @param start                where the route starts on the road map
 * @param end                  where the route ends on the road map
 * @param roadSegments         how many road pieces the route uses
 * @param intersectionsVisited how many intersections the search looked at
 * @param geometry             the shape of the route along the real roads, as an encoded polyline
 *                             (the map page decodes it to draw the route)
 */
public record RouteResponse(double distanceKm, double durationMinutes, SnappedPoint start, SnappedPoint end,
                            int roadSegments, int intersectionsVisited, String geometry) {

    /**
     * @param nodeId        the OSM id of the intersection
     * @param location      where the intersection is
     * @param snapDistanceM how far the requested point is from it
     */
    public record SnappedPoint(long nodeId, Coordinate location, double snapDistanceM) {

        static SnappedPoint from(Snap snap) {
            return new SnappedPoint(snap.node().id(), snap.node().location(), round(snap.distanceM(), 1));
        }
    }

    static RouteResponse from(RoutePlan plan) {
        return new RouteResponse(
                round(plan.route().lengthM() / 1000, 2),
                round(plan.route().travelTimeS() / 60, 1),
                SnappedPoint.from(plan.start()),
                SnappedPoint.from(plan.end()),
                plan.route().edges().size(),
                plan.route().nodesVisited(),
                PolylineEncoder.encode(plan.path()));
    }

    private static double round(double value, int decimals) {
        double scale = Math.pow(10, decimals);
        return Math.round(value * scale) / scale;
    }
}

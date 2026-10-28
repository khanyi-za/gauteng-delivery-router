package com.khanyi.java.gautenglogistics.ingest;

import com.khanyi.java.gautenglogistics.graph.RoadClass;

/**
 * A directed road segment between two intersections, built from part of a {@link DrivableWay}.
 * It gets its database id later, once unreachable parts of the graph have been removed.
 *
 * @param fromNode    OSM id of the intersection the segment starts at
 * @param toNode      OSM id of the intersection it ends at
 * @param osmWayId    the OSM way it was cut from
 * @param roadClass   the OSM highway type
 * @param lengthM     length along every point of the road, in metres
 * @param travelTimeS time to drive it at the road's speed, in seconds (the routing weight)
 * @param geometry    every point along the segment as an encoded polyline, in the direction of travel
 */
public record RawEdge(long fromNode, long toNode, long osmWayId, RoadClass roadClass,
                      double lengthM, double travelTimeS, String geometry) {
}

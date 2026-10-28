package com.khanyi.java.gautenglogistics.graph;

import com.khanyi.java.gautenglogistics.geo.Coordinate;

/**
 * An intersection (or the end of a road) on the road map.
 *
 * @param id       the OpenStreetMap id of the point
 * @param location where it is on the earth
 */
public record RoadNode(long id, Coordinate location) {
}
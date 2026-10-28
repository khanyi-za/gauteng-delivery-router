package com.khanyi.java.gautenglogistics.graph;

/**
 * A one-way piece of road from one intersection to the next. A two-way road is two edges,
 * one for each direction. The shape of the road for drawing on a map stays in the database,
 * because routing doesn't need it.
 *
 * @param id          the edge's row id in road_edge, used to fetch its shape later
 * @param from        id of the intersection it starts at
 * @param to          id of the intersection it ends at
 * @param roadClass   the kind of road, e.g. residential or motorway
 * @param lengthM     length in metres
 * @param travelTimeS seconds to drive it; this is what the route search adds up
 */
public record Edge(long id, long from, long to, RoadClass roadClass, double lengthM, double travelTimeS) {
}

package com.khanyi.java.gautenglogistics.ingest;

import com.khanyi.java.gautenglogistics.graph.RoadClass;

/**
 * A road from the OSM file that a delivery vehicle can use, before it is split into edges.
 * Node ids are always in the direction of travel for one-way roads.
 *
 * @param osmId     the OSM way id
 * @param nodeIds   every node along the road, not only intersections, so length and geometry follow its curves
 * @param roadClass the OSM highway type
 * @param speedKmh  the speed used to calculate travel time
 * @param oneWay    true if traffic may only travel from the first node towards the last
 */
public record DrivableWay(long osmId, long[] nodeIds, RoadClass roadClass, double speedKmh, boolean oneWay) {
}

package com.khanyi.java.gautenglogistics.graph;

import com.khanyi.java.gautenglogistics.geo.Coordinate;
import com.khanyi.java.gautenglogistics.geo.Haversine;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class GridIndexTest {

    @Test
    void findsTheClosestIntersection() {
        RoadGraph graph = graph(
                List.of(node(1, -26.100, 28.000), node(2, -26.100, 28.010), node(3, -26.110, 28.000)),
                List.of(road(1, 2, RoadClass.RESIDENTIAL), road(2, 3, RoadClass.RESIDENTIAL)));
        GridIndex index = new GridIndex(graph, 2_000);

        Snap snap = index.nearest(new Coordinate(-26.1001, 28.0098)).orElseThrow();

        assertThat(snap.node().id()).isEqualTo(2);
        assertThat(snap.distanceM()).isLessThan(30);
    }

    @Test
    void neverSnapsToAFreeway() {
        // Intersection 2 is only on a motorway, so the point next to it gets 1 instead
        RoadGraph graph = graph(
                List.of(node(1, -26.100, 28.000), node(2, -26.100, 28.010), node(3, -26.100, 28.020)),
                List.of(road(1, 3, RoadClass.RESIDENTIAL), road(2, 3, RoadClass.MOTORWAY)));
        GridIndex index = new GridIndex(graph, 5_000);

        Snap snap = index.nearest(new Coordinate(-26.100, 28.0101)).orElseThrow();

        assertThat(snap.node().id()).isNotEqualTo(2L);
    }

    @Test
    void findsNothingWhenEveryRoadIsTooFarAway() {
        RoadGraph graph = graph(
                List.of(node(1, -26.100, 28.000), node(2, -26.100, 28.010)),
                List.of(road(1, 2, RoadClass.RESIDENTIAL)));
        GridIndex index = new GridIndex(graph, 2_000);

        // Cape Town
        assertThat(index.nearest(new Coordinate(-33.92, 18.42))).isEmpty();
    }

    @Test
    void givesTheSameAnswerAsCheckingEveryIntersection() {
        // Scatter intersections over a few kilometres, then compare with a slow search through all of them
        Random random = new Random(42);
        List<RoadNode> nodes = new ArrayList<>();
        List<Edge> edges = new ArrayList<>();
        for (int i = 0; i < 2_000; i++) {
            nodes.add(node(i, -26.1 + random.nextDouble() * 0.05, 28.0 + random.nextDouble() * 0.05));
            if (i > 0) {
                edges.add(road(i - 1, i, RoadClass.RESIDENTIAL));
            }
        }
        GridIndex index = new GridIndex(graph(nodes, edges), 5_000);

        for (int i = 0; i < 200; i++) {
            Coordinate point = new Coordinate(-26.1 + random.nextDouble() * 0.05, 28.0 + random.nextDouble() * 0.05);
            double closestM = nodes.stream()
                    .mapToDouble(node -> Haversine.distanceMetres(point, node.location()))
                    .min().orElseThrow();

            assertThat(index.nearest(point).orElseThrow().distanceM()).isEqualTo(closestM);
        }
    }

    private static RoadGraph graph(List<RoadNode> nodes, List<Edge> edges) {
        return new RoadGraph(nodes, edges);
    }

    private static RoadNode node(long id, double lat, double lon) {
        return new RoadNode(id, new Coordinate(lat, lon));
    }

    private static Edge road(long from, long to, RoadClass roadClass) {
        return new Edge(from * 1_000 + to, from, to, roadClass, 100, 10);
    }
}

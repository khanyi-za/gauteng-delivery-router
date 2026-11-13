package com.khanyi.java.gautenglogistics.routing;

import com.khanyi.java.gautenglogistics.geo.Coordinate;
import com.khanyi.java.gautenglogistics.geo.Haversine;
import com.khanyi.java.gautenglogistics.graph.Edge;
import com.khanyi.java.gautenglogistics.graph.RoadClass;
import com.khanyi.java.gautenglogistics.graph.RoadGraph;
import com.khanyi.java.gautenglogistics.graph.RoadNode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class AStarTest {

    /** How far apart the points on the test maps are, in degrees of latitude (about 111 m). */
    private static final double STEP = 0.001;

    /**
     * A straight north-south road with the start in the middle.
     *
     * <pre>
     *   -5 ... -2  -1   0   1   2 ... 5      0 is the start, 5 the destination
     * </pre>
     *
     * Dijkstra spreads out both ways. A* knows the points south of 0 head away from 5.
     */
    @Test
    void leavesAloneRoadsThatHeadAwayFromTheDestination() {
        List<RoadNode> nodes = new ArrayList<>();
        List<Edge> edges = new ArrayList<>();
        for (int i = -5; i <= 5; i++) {
            nodes.add(node(i, i * STEP, 0));
        }
        for (int i = -5; i < 5; i++) {
            addTwoWayRoad(edges, nodes.get(i + 5), nodes.get(i + 6), RoadClass.MAX_SPEED_KMH, 1);
        }
        RoadGraph graph = new RoadGraph(nodes, edges);

        Route aStar = AStar.fastestRoute(graph, 0, 5).orElseThrow();
        Route dijkstra = Dijkstra.fastestRoute(graph, 0, 5).orElseThrow();

        assertThat(aStar.edges()).isEqualTo(dijkstra.edges());
        assertThat(aStar.nodesVisited()).isEqualTo(6); // only 0 to 5
        assertThat(dijkstra.nodesVisited()).isGreaterThan(6);
    }

    @Test
    void findsNoRouteWhenThereIsNone() {
        RoadGraph graph = new RoadGraph(List.of(node(1, 0, 0), node(2, STEP, 0)), List.of());

        assertThat(AStar.fastestRoute(graph, 1, 2)).isEmpty();
    }

    @Test
    void aRouteToTheSamePlaceIsEmpty() {
        RoadGraph graph = new RoadGraph(List.of(node(1, 0, 0)), List.of());

        Route route = AStar.fastestRoute(graph, 1, 1).orElseThrow();

        assertThat(route.edges()).isEmpty();
        assertThat(route.travelTimeS()).isZero();
    }

    /**
     * A 30 by 30 grid of streets with random speeds, some bendy and some one-way. A* must find
     * routes exactly as fast as Dijkstra's, while looking at fewer intersections.
     */
    @Test
    void findsRoutesAsFastAsDijkstraWhileLookingAtFewerIntersections() {
        Random random = new Random(42);
        int size = 30;
        List<RoadNode> nodes = new ArrayList<>();
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                nodes.add(node(row * size + col, row * STEP, col * STEP));
            }
        }
        List<Edge> edges = new ArrayList<>();
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                RoadNode here = nodes.get(row * size + col);
                if (col + 1 < size) {
                    addRandomRoad(edges, here, nodes.get(row * size + col + 1), random);
                }
                if (row + 1 < size) {
                    addRandomRoad(edges, here, nodes.get((row + 1) * size + col), random);
                }
            }
        }
        RoadGraph graph = new RoadGraph(nodes, edges);

        long aStarVisited = 0;
        long dijkstraVisited = 0;
        for (int i = 0; i < 300; i++) {
            long from = random.nextInt(size * size);
            long to = random.nextInt(size * size);
            Optional<Route> aStar = AStar.fastestRoute(graph, from, to);
            Optional<Route> dijkstra = Dijkstra.fastestRoute(graph, from, to);

            assertThat(aStar.isPresent()).isEqualTo(dijkstra.isPresent());
            if (aStar.isPresent()) {
                assertThat(aStar.get().travelTimeS()).isCloseTo(dijkstra.get().travelTimeS(), within(1e-6));
                assertThat(aStar.get().nodesVisited()).isLessThanOrEqualTo(dijkstra.get().nodesVisited());
                aStarVisited += aStar.get().nodesVisited();
                dijkstraVisited += dijkstra.get().nodesVisited();
            }
        }
        assertThat(aStarVisited).isLessThan(dijkstraVisited);
    }

    /**
     * A road between two points with a random speed. It's up to 30% longer than the straight
     * line (it bends), and one road in five is one-way.
     */
    private static void addRandomRoad(List<Edge> edges, RoadNode a, RoadNode b, Random random) {
        double speedKmh = 20 + random.nextDouble() * (RoadClass.MAX_SPEED_KMH - 20);
        double bend = 1 + random.nextDouble() * 0.3;
        if (random.nextDouble() < 0.2) {
            edges.add(edge(edges.size(), a, b, speedKmh, bend));
        } else {
            addTwoWayRoad(edges, a, b, speedKmh, bend);
        }
    }

    private static void addTwoWayRoad(List<Edge> edges, RoadNode a, RoadNode b, double speedKmh, double bend) {
        edges.add(edge(edges.size(), a, b, speedKmh, bend));
        edges.add(edge(edges.size(), b, a, speedKmh, bend));
    }

    private static Edge edge(long id, RoadNode from, RoadNode to, double speedKmh, double bend) {
        double lengthM = Haversine.distanceMetres(from.location(), to.location()) * bend;
        return new Edge(id, from.id(), to.id(), RoadClass.RESIDENTIAL, lengthM, lengthM / (speedKmh / 3.6));
    }

    /** A point near Johannesburg, moved north and east by the given number of degrees. */
    private static RoadNode node(long id, double north, double east) {
        return new RoadNode(id, new Coordinate(-26.2 + north, 28.0 + east));
    }
}

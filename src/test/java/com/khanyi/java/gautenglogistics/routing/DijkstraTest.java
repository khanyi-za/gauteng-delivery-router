package com.khanyi.java.gautenglogistics.routing;

import com.khanyi.java.gautenglogistics.geo.Coordinate;
import com.khanyi.java.gautenglogistics.graph.Edge;
import com.khanyi.java.gautenglogistics.graph.RoadClass;
import com.khanyi.java.gautenglogistics.graph.RoadGraph;
import com.khanyi.java.gautenglogistics.graph.RoadNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Uses a tiny road map. The numbers on the roads are seconds to drive them.
 *
 * <pre>
 *        10        10
 *   1 -------> 2 -------> 3       the short way: 1 -> 2 -> 3 = 20 s, but 2 -> 3 is one-way
 *   |                     ^
 *   |  5              5   |
 *   +------> 4 -----------+       the other way: 1 -> 4 -> 3 = 10 s, faster
 *
 *   5                             an intersection no road leads to
 * </pre>
 */
class DijkstraTest {

    private static final Edge ONE_TWO = edge(12, 1, 2, 10);
    private static final Edge TWO_THREE = edge(23, 2, 3, 10);
    private static final Edge ONE_FOUR = edge(14, 1, 4, 5);
    private static final Edge FOUR_THREE = edge(43, 4, 3, 5);

    private static final RoadGraph GRAPH = new RoadGraph(
            LongStream.rangeClosed(1, 5).mapToObj(id -> new RoadNode(id, new Coordinate(-26, 28))).toList(),
            List.of(ONE_TWO, TWO_THREE, ONE_FOUR, FOUR_THREE));

    @Test
    void picksTheFastestRouteNotTheOneWithFewestRoads() {
        Route route = Dijkstra.fastestRoute(GRAPH, 1, 3).orElseThrow();

        assertThat(route.edges()).containsExactly(ONE_FOUR, FOUR_THREE);
        assertThat(route.travelTimeS()).isEqualTo(10);
        assertThat(route.lengthM()).isEqualTo(200);
    }

    @Test
    void followsOneWayStreets() {
        // Every road here is one-way, so there's no way back from 3 to 1
        assertThat(Dijkstra.fastestRoute(GRAPH, 3, 1)).isEmpty();
    }

    @Test
    void findsNoRouteToAnIntersectionNothingLeadsTo() {
        assertThat(Dijkstra.fastestRoute(GRAPH, 1, 5)).isEmpty();
    }

    @Test
    void aRouteToTheSamePlaceIsEmpty() {
        Route route = Dijkstra.fastestRoute(GRAPH, 2, 2).orElseThrow();

        assertThat(route.edges()).isEmpty();
        assertThat(route.travelTimeS()).isZero();
    }

    @Test
    void usesTheBetterRoadWhenItIsFoundLater() {
        // The search reaches 3 through 2 first (time 20), then finds the faster way through 4 (time 10)
        RoadGraph graph = new RoadGraph(
                LongStream.rangeClosed(1, 4).mapToObj(id -> new RoadNode(id, new Coordinate(-26, 28))).toList(),
                List.of(edge(12, 1, 2, 1), edge(23, 2, 3, 19), edge(14, 1, 4, 2), edge(43, 4, 3, 8)));

        Route route = Dijkstra.fastestRoute(graph, 1, 3).orElseThrow();

        assertThat(route.travelTimeS()).isEqualTo(10);
        assertThat(route.edges()).extracting(Edge::id).containsExactly(14L, 43L);
    }

    /**
     * Searching backwards on a map with a one-way street.
     *
     * <pre>
     *   3 ----1 s----> 1      one-way: 3 to 1 takes 1 s
     *   ^              |
     *   20 s           20 s   but from 1 back to 3 you have to go round through 4: 40 s
     *   |              v
     *   +---- 4 <------+
     *
     *   2 <--15 s--> 3        2 and 3 are joined by a two-way road, 15 s each way
     * </pre>
     *
     * 1 is closer to 3 if you drive away from 3, but 2 is closer if you drive TO 3.
     */
    private static final RoadGraph ONE_WAY_GRAPH = new RoadGraph(
            LongStream.rangeClosed(1, 5).mapToObj(id -> new RoadNode(id, new Coordinate(-26, 28))).toList(),
            List.of(edge(31, 3, 1, 1), edge(14, 1, 4, 20), edge(43, 4, 3, 20),
                    edge(23, 2, 3, 15), edge(32, 3, 2, 15)));

    @Test
    void searchingBackwardsFindsThePlaceWithTheFastestDriveToTheDestination() {
        Route route = Dijkstra.fastestRouteFromNearest(ONE_WAY_GRAPH, 3, node -> node == 1 || node == 2).orElseThrow();

        assertThat(route.edges()).extracting(Edge::id).containsExactly(23L);
        assertThat(route.travelTimeS()).isEqualTo(15);
    }

    @Test
    void searchingBackwardsGivesTheRouteInDrivingOrder() {
        Route route = Dijkstra.fastestRouteFromNearest(ONE_WAY_GRAPH, 3, node -> node == 1).orElseThrow();

        assertThat(route.edges()).extracting(Edge::id).containsExactly(14L, 43L);
        assertThat(route.travelTimeS()).isEqualTo(40);
        assertThat(route.lengthM()).isEqualTo(200);
    }

    @Test
    void searchingBackwardsFromAPlaceThatIsAlreadyTheGoalIsAnEmptyRoute() {
        Route route = Dijkstra.fastestRouteFromNearest(ONE_WAY_GRAPH, 3, node -> node == 3).orElseThrow();

        assertThat(route.edges()).isEmpty();
    }

    @Test
    void searchingBackwardsFindsNothingWhenNoGoalCanReachTheDestination() {
        assertThat(Dijkstra.fastestRouteFromNearest(ONE_WAY_GRAPH, 3, node -> node == 5)).isEmpty();
    }

    private static Edge edge(long id, long from, long to, double seconds) {
        return new Edge(id, from, to, RoadClass.RESIDENTIAL, 100, seconds);
    }
}

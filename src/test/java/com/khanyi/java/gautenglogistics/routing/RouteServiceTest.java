package com.khanyi.java.gautenglogistics.routing;

import com.khanyi.java.gautenglogistics.geo.Coordinate;
import com.khanyi.java.gautenglogistics.graph.Edge;
import com.khanyi.java.gautenglogistics.graph.GridIndex;
import com.khanyi.java.gautenglogistics.graph.RoadClass;
import com.khanyi.java.gautenglogistics.graph.RoadGraph;
import com.khanyi.java.gautenglogistics.graph.RoadNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** A straight two-way road through three intersections, about 1 km apart. */
class RouteServiceTest {

    private static final RoadNode WEST = new RoadNode(1, new Coordinate(-26.10, 28.00));
    private static final RoadNode MIDDLE = new RoadNode(2, new Coordinate(-26.10, 28.01));
    private static final RoadNode EAST = new RoadNode(3, new Coordinate(-26.10, 28.02));

    private final RoadGraph graph = new RoadGraph(List.of(WEST, MIDDLE, EAST), List.of(
            edge(12, 1, 2), edge(21, 2, 1), edge(23, 2, 3), edge(32, 3, 2)));
    private final RouteService service = new RouteService(graph, new GridIndex(graph, 2_000));

    @Test
    void matchesGpsPointsToRoadsAndFindsTheRoute() {
        RoutePlan plan = service.plan(new Coordinate(-26.1003, 28.0001), new Coordinate(-26.0998, 28.0199));

        assertThat(plan.start().node()).isEqualTo(WEST);
        assertThat(plan.end().node()).isEqualTo(EAST);
        assertThat(plan.route().edges()).extracting(Edge::id).containsExactly(12L, 23L);
        assertThat(plan.path()).containsExactly(WEST.location(), MIDDLE.location(), EAST.location());
    }

    @Test
    void refusesAPointFarFromAnyRoad() {
        Coordinate capeTown = new Coordinate(-33.92, 18.42);

        assertThatThrownBy(() -> service.plan(capeTown, EAST.location()))
                .isInstanceOf(NoRoadNearbyException.class)
                .hasMessageContaining("-33.92");
    }

    private static Edge edge(long id, long from, long to) {
        return new Edge(id, from, to, RoadClass.RESIDENTIAL, 1_000, 60);
    }
}

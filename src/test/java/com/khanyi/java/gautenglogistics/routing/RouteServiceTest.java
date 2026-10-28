package com.khanyi.java.gautenglogistics.routing;

import com.khanyi.java.gautenglogistics.geo.Coordinate;
import com.khanyi.java.gautenglogistics.geo.PolylineEncoder;
import com.khanyi.java.gautenglogistics.graph.Edge;
import com.khanyi.java.gautenglogistics.graph.EdgeGeometryLoader;
import com.khanyi.java.gautenglogistics.graph.GridIndex;
import com.khanyi.java.gautenglogistics.graph.RoadClass;
import com.khanyi.java.gautenglogistics.graph.RoadGraph;
import com.khanyi.java.gautenglogistics.graph.RoadNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * A straight two-way road through three intersections, about 1 km apart. The road shapes come
 * from a pretend EdgeGeometryLoader, so no database is needed.
 */
class RouteServiceTest {

    private static final RoadNode WEST = new RoadNode(1, new Coordinate(-26.10, 28.00));
    private static final RoadNode MIDDLE = new RoadNode(2, new Coordinate(-26.10, 28.01));
    private static final RoadNode EAST = new RoadNode(3, new Coordinate(-26.10, 28.02));

    /** A bend in the road between WEST and MIDDLE, which isn't an intersection. */
    private static final Coordinate BEND = new Coordinate(-26.09, 28.005);

    private final RoadGraph graph = new RoadGraph(List.of(WEST, MIDDLE, EAST), List.of(
            edge(12, 1, 2), edge(21, 2, 1), edge(23, 2, 3), edge(32, 3, 2)));
    private final EdgeGeometryLoader geometryLoader = mock(EdgeGeometryLoader.class);
    private final RouteService service = new RouteService(graph, new GridIndex(graph, 2_000), geometryLoader);

    @Test
    void matchesGpsPointsToRoadsAndFollowsTheRealRoadShape() {
        given(geometryLoader.load(any())).willReturn(Map.of(
                12L, PolylineEncoder.encode(List.of(WEST.location(), BEND, MIDDLE.location())),
                23L, PolylineEncoder.encode(List.of(MIDDLE.location(), EAST.location()))));

        RoutePlan plan = service.plan(new Coordinate(-26.1003, 28.0001), new Coordinate(-26.0998, 28.0199));

        assertThat(plan.start().node()).isEqualTo(WEST);
        assertThat(plan.end().node()).isEqualTo(EAST);
        assertThat(plan.route().edges()).extracting(Edge::id).containsExactly(12L, 23L);
        // The bend is included, and MIDDLE appears once even though both pieces touch it
        assertThat(plan.path()).containsExactly(WEST.location(), BEND, MIDDLE.location(), EAST.location());
    }

    @Test
    void startAndEndAtTheSameIntersectionIsAOnePointRoute() {
        given(geometryLoader.load(any())).willReturn(Map.of());

        RoutePlan plan = service.plan(MIDDLE.location(), MIDDLE.location());

        assertThat(plan.route().edges()).isEmpty();
        assertThat(plan.path()).containsExactly(MIDDLE.location());
    }

    @Test
    void aMissingRoadShapeIsABug() {
        given(geometryLoader.load(any())).willReturn(Map.of());

        assertThatThrownBy(() -> service.plan(WEST.location(), EAST.location()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("road piece 12");
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

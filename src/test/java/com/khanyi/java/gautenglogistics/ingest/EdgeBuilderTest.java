package com.khanyi.java.gautenglogistics.ingest;

import com.khanyi.java.gautenglogistics.geo.Coordinate;
import com.khanyi.java.gautenglogistics.geo.Haversine;
import com.khanyi.java.gautenglogistics.graph.RoadClass;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Uses a tiny hand-made road network:
 *
 * <pre>
 *   1 ---- 2 ---- 3 ---- 4      main road (two-way): nodes 1, 2, 3, 4
 *                 |
 *                 5             side road (one-way, 5 -> 3)
 * </pre>
 *
 * Node 2 is only a bend in the main road. Node 3 is shared by both roads, so it's an intersection.
 */
class EdgeBuilderTest {

    private static final Map<Long, Coordinate> COORDINATES = Map.of(
            1L, new Coordinate(-26.100, 28.000),
            2L, new Coordinate(-26.100, 28.001),
            3L, new Coordinate(-26.101, 28.002),
            4L, new Coordinate(-26.101, 28.003),
            5L, new Coordinate(-26.102, 28.002));

    private static final DrivableWay MAIN_ROAD =
            new DrivableWay(100, new long[]{1, 2, 3, 4}, RoadClass.RESIDENTIAL, 60, false);
    private static final DrivableWay SIDE_ROAD =
            new DrivableWay(200, new long[]{5, 3}, RoadClass.SERVICE, 30, true);

    @Test
    void roadEndsAndSharedNodesAreVerticesButBendsAreNot() {
        Map<Long, Integer> uses = EdgeBuilder.countNodeUses(List.of(MAIN_ROAD, SIDE_ROAD));

        assertThat(EdgeBuilder.isVertex(1, uses)).isTrue();  // end of main road
        assertThat(EdgeBuilder.isVertex(2, uses)).isFalse(); // only a bend
        assertThat(EdgeBuilder.isVertex(3, uses)).isTrue();  // shared by both roads
        assertThat(EdgeBuilder.isVertex(4, uses)).isTrue();  // end of main road
        assertThat(EdgeBuilder.isVertex(5, uses)).isTrue();  // end of side road
        assertThat(uses.keySet()).containsExactlyInAnyOrder(1L, 2L, 3L, 4L, 5L);
    }

    @Test
    void splitsRoadsAtIntersectionsInTheRightDirections() {
        List<RawEdge> edges = build(MAIN_ROAD, SIDE_ROAD);

        assertThat(edges).extracting(edge -> edge.fromNode() + "->" + edge.toNode())
                .containsExactlyInAnyOrder("1->3", "3->1", "3->4", "4->3", "5->3");
    }

    @Test
    void lengthFollowsTheBendNotTheStraightLine() {
        RawEdge oneToThree = find(build(MAIN_ROAD, SIDE_ROAD), 1, 3);

        double viaBend = Haversine.distanceMetres(COORDINATES.get(1L), COORDINATES.get(2L))
                + Haversine.distanceMetres(COORDINATES.get(2L), COORDINATES.get(3L));
        double straight = Haversine.distanceMetres(COORDINATES.get(1L), COORDINATES.get(3L));

        assertThat(oneToThree.lengthM()).isCloseTo(viaBend, within(0.001));
        assertThat(oneToThree.lengthM()).isGreaterThan(straight);
    }

    @Test
    void travelTimeIsLengthDividedBySpeed() {
        RawEdge fiveToThree = find(build(MAIN_ROAD, SIDE_ROAD), 5, 3);

        double expectedSeconds = fiveToThree.lengthM() / (30 / 3.6);
        assertThat(fiveToThree.travelTimeS()).isCloseTo(expectedSeconds, within(0.001));
    }

    @Test
    void reverseEdgeHasTheSameLengthAndReversedGeometry() {
        List<RawEdge> edges = build(MAIN_ROAD, SIDE_ROAD);
        RawEdge forward = find(edges, 1, 3);
        RawEdge backward = find(edges, 3, 1);

        assertThat(backward.lengthM()).isEqualTo(forward.lengthM());
        assertThat(backward.geometry()).isNotEqualTo(forward.geometry());
    }

    @Test
    void dropsTheStretchOfRoadWithAMissingCoordinate() {
        Map<Long, Coordinate> withoutBend = new HashMap<>(COORDINATES);
        withoutBend.remove(2L); // as if node 2 were outside the map extract

        List<RawEdge> edges = EdgeBuilder.buildEdges(List.of(MAIN_ROAD, SIDE_ROAD),
                EdgeBuilder.countNodeUses(List.of(MAIN_ROAD, SIDE_ROAD)), withoutBend);

        assertThat(edges).extracting(edge -> edge.fromNode() + "->" + edge.toNode())
                .containsExactlyInAnyOrder("3->4", "4->3", "5->3");
    }

    @Test
    void dropsEdgesThatStartAndEndAtTheSameNode() {
        DrivableWay loop = new DrivableWay(300, new long[]{1, 2, 1}, RoadClass.RESIDENTIAL, 60, true);

        assertThat(build(loop)).isEmpty();
    }

    private static List<RawEdge> build(DrivableWay... ways) {
        List<DrivableWay> list = List.of(ways);
        return EdgeBuilder.buildEdges(list, EdgeBuilder.countNodeUses(list), COORDINATES);
    }

    private static RawEdge find(List<RawEdge> edges, long from, long to) {
        return edges.stream()
                .filter(edge -> edge.fromNode() == from && edge.toNode() == to)
                .findFirst()
                .orElseThrow();
    }
}

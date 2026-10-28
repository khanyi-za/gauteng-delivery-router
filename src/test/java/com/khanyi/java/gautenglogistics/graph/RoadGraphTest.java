package com.khanyi.java.gautenglogistics.graph;

import com.khanyi.java.gautenglogistics.geo.Coordinate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Uses a tiny road map:
 *
 * <pre>
 *   1 <---> 2 ----> 3      1-2 is a two-way road (two edges), 2-3 is one-way
 * </pre>
 */
class RoadGraphTest {

    private static final List<RoadNode> NODES = List.of(
            new RoadNode(1, new Coordinate(-26.1, 28.0)),
            new RoadNode(2, new Coordinate(-26.1, 28.1)),
            new RoadNode(3, new Coordinate(-26.1, 28.2)));

    private static final Edge ONE_TO_TWO = edge(10, 1, 2);
    private static final Edge TWO_TO_ONE = edge(11, 2, 1);
    private static final Edge TWO_TO_THREE = edge(12, 2, 3);

    private final RoadGraph graph = new RoadGraph(NODES, List.of(ONE_TO_TWO, TWO_TO_ONE, TWO_TO_THREE));

    @Test
    void findsTheRoadsLeavingAnIntersection() {
        assertThat(graph.outgoing(2)).containsExactlyInAnyOrder(TWO_TO_ONE, TWO_TO_THREE);
        assertThat(graph.outgoing(1)).containsExactly(ONE_TO_TWO);
    }

    @Test
    void findsTheRoadsArrivingAtAnIntersection() {
        assertThat(graph.incoming(2)).containsExactly(ONE_TO_TWO);
        assertThat(graph.incoming(1)).containsExactly(TWO_TO_ONE);
        assertThat(graph.incoming(3)).containsExactly(TWO_TO_THREE);
    }

    @Test
    void aDeadEndHasNoRoadsLeavingIt() {
        // You can drive into 3 but not out: one-way streets make outgoing and incoming different
        assertThat(graph.outgoing(3)).isEmpty();
    }

    @Test
    void looksUpIntersectionsAndCountsEverything() {
        assertThat(graph.node(3).location()).isEqualTo(new Coordinate(-26.1, 28.2));
        assertThat(graph.node(99)).isNull();
        assertThat(graph.nodeCount()).isEqualTo(3);
        assertThat(graph.edgeCount()).isEqualTo(3);
    }

    @Test
    void refusesAnEdgeToAnUnknownIntersection() {
        assertThatThrownBy(() -> new RoadGraph(NODES, List.of(edge(20, 1, 99))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("20");
    }

    private static Edge edge(long id, long from, long to) {
        return new Edge(id, from, to, RoadClass.RESIDENTIAL, 100, 6);
    }
}

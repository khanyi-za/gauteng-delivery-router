package com.khanyi.java.gautenglogistics.ingest;

import com.khanyi.java.gautenglogistics.graph.RoadClass;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LargestComponentFilterTest {

    /**
     * <pre>
     *   1 <-> 2 <-> 3        two-way roads: the main network
     *   3  -> 4              one-way into a dead end: you can get in but not out
     *   7 <-> 8              a separate pair of streets: smaller than the main network
     * </pre>
     */
    @Test
    void keepsOnlyEdgesWhereYouCanGetThereAndBack() {
        List<RawEdge> edges = List.of(
                edge(1, 2), edge(2, 1),
                edge(2, 3), edge(3, 2),
                edge(3, 4),
                edge(7, 8), edge(8, 7));

        List<RawEdge> kept = LargestComponentFilter.keepLargestComponent(edges);

        assertThat(kept).containsExactly(edge(1, 2), edge(2, 1), edge(2, 3), edge(3, 2));
    }

    private static RawEdge edge(long from, long to) {
        return new RawEdge(from, to, 1, RoadClass.RESIDENTIAL, 100, 6, "");
    }
}

package com.khanyi.java.gautenglogistics.graph;

import com.khanyi.java.gautenglogistics.TestcontainersConfiguration;
import com.khanyi.java.gautenglogistics.geo.Coordinate;
import com.khanyi.java.gautenglogistics.ingest.RawEdge;
import com.khanyi.java.gautenglogistics.ingest.RoadNetworkWriter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Saves a small road map with the real writer, then checks the loader reads back the same thing. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class RoadGraphLoaderTest {

    @Autowired
    private RoadNetworkWriter writer;

    @Autowired
    private RoadGraphLoader loader;

    @Test
    void loadsWhatTheImportSaved() {
        writer.replaceNetwork(List.of(
                        new RawEdge(1, 2, 100, RoadClass.PRIMARY, 500, 22.5, "abc"),
                        new RawEdge(2, 1, 100, RoadClass.PRIMARY, 500, 22.5, "cba"),
                        new RawEdge(2, 3, 200, RoadClass.SERVICE, 50, 6, "xyz")),
                Map.of(1L, new Coordinate(-26.1, 28.0),
                        2L, new Coordinate(-26.2, 28.1),
                        3L, new Coordinate(-26.3, 28.2)));

        RoadGraph graph = loader.load();

        assertThat(graph.nodeCount()).isEqualTo(3);
        assertThat(graph.edgeCount()).isEqualTo(3);
        assertThat(graph.node(2).location()).isEqualTo(new Coordinate(-26.2, 28.1));
        // Edge ids are the positions in the saved list: 0, 1, 2
        assertThat(graph.outgoing(2)).containsExactlyInAnyOrder(
                new Edge(1, 2, 1, RoadClass.PRIMARY, 500, 22.5),
                new Edge(2, 2, 3, RoadClass.SERVICE, 50, 6));
        assertThat(graph.incoming(3)).extracting(Edge::from).containsExactly(2L);
    }
}

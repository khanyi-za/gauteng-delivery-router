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

/** Saves a small road map with the real writer, then reads some of the road shapes back. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class EdgeGeometryLoaderTest {

    @Autowired
    private RoadNetworkWriter writer;

    @Autowired
    private EdgeGeometryLoader loader;

    @Test
    void loadsTheShapesOfTheRequestedRoadPieces() {
        writer.replaceNetwork(List.of(
                        new RawEdge(1, 2, 100, RoadClass.PRIMARY, 500, 22.5, "abc"),
                        new RawEdge(2, 1, 100, RoadClass.PRIMARY, 500, 22.5, "cba"),
                        new RawEdge(2, 3, 200, RoadClass.SERVICE, 50, 6, "xyz")),
                Map.of(1L, new Coordinate(-26.1, 28.0),
                        2L, new Coordinate(-26.2, 28.1),
                        3L, new Coordinate(-26.3, 28.2)));

        // Edge ids are the positions in the saved list: 0, 1, 2. Id 99 doesn't exist.
        assertThat(loader.load(List.of(0L, 2L, 99L))).isEqualTo(Map.of(0L, "abc", 2L, "xyz"));
    }

    @Test
    void noIdsMeansNoQuery() {
        assertThat(loader.load(List.of())).isEmpty();
    }
}

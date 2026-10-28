package com.khanyi.java.gautenglogistics.ingest;

import com.khanyi.java.gautenglogistics.TestcontainersConfiguration;
import com.khanyi.java.gautenglogistics.geo.Coordinate;
import com.khanyi.java.gautenglogistics.graph.RoadClass;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Runs against a real Postgres in a container, with the Flyway schema applied. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class RoadNetworkWriterTest {

    private static final Map<Long, Coordinate> COORDINATES = Map.of(
            1L, new Coordinate(-26.1, 28.0),
            2L, new Coordinate(-26.2, 28.1),
            3L, new Coordinate(-26.3, 28.2));

    @Autowired
    private RoadNetworkWriter writer;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void savesNodesAndNumbersEdgesFromZero() {
        List<RawEdge> edges = List.of(
                new RawEdge(1, 2, 100, RoadClass.PRIMARY, 500, 22.5, "abc"),
                new RawEdge(2, 1, 100, RoadClass.PRIMARY, 500, 22.5, "cba"));

        int nodes = writer.replaceNetwork(edges, COORDINATES);

        assertThat(nodes).isEqualTo(2);
        assertThat(jdbc.queryForList("SELECT id FROM road_node ORDER BY id", Long.class)).containsExactly(1L, 2L);
        assertThat(jdbc.queryForMap("SELECT * FROM road_edge WHERE id = 1"))
                .containsEntry("from_node", 2L)
                .containsEntry("to_node", 1L)
                .containsEntry("osm_way_id", 100L)
                .containsEntry("road_class", "primary")
                .containsEntry("length_m", 500.0)
                .containsEntry("travel_time_s", 22.5)
                .containsEntry("geometry", "cba");
        assertThat(jdbc.queryForObject("SELECT lat FROM road_node WHERE id = 2", Double.class)).isEqualTo(-26.2);
    }

    @Test
    void replacesThePreviousNetwork() {
        writer.replaceNetwork(List.of(new RawEdge(1, 2, 100, RoadClass.PRIMARY, 500, 22.5, "abc")), COORDINATES);

        writer.replaceNetwork(List.of(new RawEdge(2, 3, 200, RoadClass.SERVICE, 50, 6, "xyz")), COORDINATES);

        assertThat(jdbc.queryForList("SELECT id FROM road_node ORDER BY id", Long.class)).containsExactly(2L, 3L);
        assertThat(jdbc.queryForList("SELECT osm_way_id FROM road_edge", Long.class)).containsExactly(200L);
    }
}

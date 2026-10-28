package com.khanyi.java.gautenglogistics.graph;


import com.khanyi.java.gautenglogistics.geo.Coordinate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/** Reads the saved road map from the road_node and road_edge tables. */
@Repository
public class RoadGraphLoader {

    private final JdbcTemplate jdbc;

    public RoadGraphLoader(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public RoadGraph load() {
        List<RoadNode> nodes = jdbc.query("SELECT id, lat, lon FROM road_node",
                (rs, row) -> new RoadNode(rs.getLong("id"),
                        new Coordinate(rs.getDouble("lat"), rs.getDouble("lon"))));

        // The road shapes (geometry) are left out: they're big and only needed to draw a finished route
        List<Edge> edges = jdbc.query("""
                          SELECT id, from_node, to_node, road_class, length_m, travel_time_s
                          FROM road_edge""",
                (rs, row) -> new Edge(rs.getLong("id"), rs.getLong("from_node"), rs.getLong("to_node"),
                        RoadClass.fromTag(rs.getString("road_class")).orElseThrow(),
                        rs.getDouble("length_m"), rs.getDouble("travel_time_s")));

        return new RoadGraph(nodes, edges);
    }
}

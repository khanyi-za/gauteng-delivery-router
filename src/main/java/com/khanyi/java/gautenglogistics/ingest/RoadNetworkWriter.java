package com.khanyi.java.gautenglogistics.ingest;

import com.khanyi.java.gautenglogistics.geo.Coordinate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Saves the road map to the road_node and road_edge tables, sending the rows in big groups.
 * There are far too many rows to save them one by one as JPA entities.
 */
@Repository
public class RoadNetworkWriter {

    private static final int BATCH_SIZE = 5_000;

    private final JdbcTemplate jdbc;

    public RoadNetworkWriter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Deletes the saved road map and saves this one instead. It all happens in one transaction,
     * so if something fails halfway, the old road map is kept. Road piece i is saved with id i.
     *
     * @return the number of intersections saved
     */
    @Transactional
    public int replaceNetwork(List<RawEdge> edges, Map<Long, Coordinate> coordinates) {
        jdbc.execute("TRUNCATE road_edge, road_node");

        Set<Long> nodeIds = new LinkedHashSet<>();
        for (RawEdge edge : edges) {
            nodeIds.add(edge.fromNode());
            nodeIds.add(edge.toNode());
        }

        // Intersections first, because the road pieces point to them
        jdbc.batchUpdate("INSERT INTO road_node (id, lat, lon) VALUES (?, ?, ?)",
                new ArrayList<>(nodeIds), BATCH_SIZE, (ps, id) -> {
                    Coordinate coordinate = coordinates.get(id);
                    ps.setLong(1, id);
                    ps.setDouble(2, coordinate.lat());
                    ps.setDouble(3, coordinate.lon());
                });

        List<Integer> edgeIds = IntStream.range(0, edges.size()).boxed().toList();
        jdbc.batchUpdate("""
                          INSERT INTO road_edge (id, from_node, to_node, osm_way_id, road_class,
                                                 length_m, travel_time_s, geometry)
                          VALUES (?, ?, ?, ?, ?, ?, ?, ?)""",
                edgeIds, BATCH_SIZE, (ps, id) -> {
                    RawEdge edge = edges.get(id);
                    ps.setLong(1, id);
                    ps.setLong(2, edge.fromNode());
                    ps.setLong(3, edge.toNode());
                    ps.setLong(4, edge.osmWayId());
                    ps.setString(5, edge.roadClass().tag());
                    ps.setDouble(6, edge.lengthM());
                    ps.setDouble(7, edge.travelTimeS());
                    ps.setString(8, edge.geometry());
                });

        return nodeIds.size();
    }
}

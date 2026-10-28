package com.khanyi.java.gautenglogistics.graph;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Reads the shapes of road pieces from the road_edge table. They aren't kept in memory with the
 * rest of the map because they're big and only needed to draw a finished route.
 */
@Repository
public class EdgeGeometryLoader  {

    private final JdbcTemplate jdbc;

    public EdgeGeometryLoader(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * @return the shape of each road piece as an encoded polyline, by road piece id.
     *         Ids that aren't in the table are left out.
     */
    public Map<Long, String> load(Collection<Long> edgeIds) {
        Map<Long, String> geometries = new HashMap<>();
        if (edgeIds.isEmpty()) {
            return geometries;
        }
        // One query for the whole route: all the ids go in as a single Postgres array
        jdbc.query("SELECT id, geometry FROM road_edge WHERE id = ANY (?)",
                ps -> ps.setArray(1, ps.getConnection().createArrayOf("bigint", edgeIds.toArray())),
                rs -> {
                    geometries.put(rs.getLong("id"), rs.getString("geometry"));
                });
        return geometries;
    }
}

-- Loaded with JDBC batching rather than JPA: there are far too many rows for the ORM.
-- Only nodes in the largest strongly connected component are stored.

-- A road intersection (or dead end). id is the OSM node id.
CREATE TABLE road_node (
                           id  BIGINT PRIMARY KEY,
                           lat DOUBLE PRECISION NOT NULL,
                           lon DOUBLE PRECISION NOT NULL
);

-- A directed road segment between two intersections.
-- Two-way roads are stored as two rows, one per direction.
CREATE TABLE road_edge (
                           id            BIGINT PRIMARY KEY,
                           from_node     BIGINT           NOT NULL REFERENCES road_node (id),
                           to_node       BIGINT           NOT NULL REFERENCES road_node (id),
                           osm_way_id    BIGINT           NOT NULL,
                           road_class    VARCHAR(32)      NOT NULL, -- OSM highway tag, e.g. motorway, residential
                           length_m      DOUBLE PRECISION NOT NULL, -- summed along every point of the road, not straight-line
                           travel_time_s DOUBLE PRECISION NOT NULL, -- the routing weight
                           geometry      TEXT             NOT NULL  -- encoded polyline in the direction of travel
);

CREATE INDEX idx_road_edge_from_node ON road_edge (from_node);
CREATE INDEX idx_road_edge_to_node ON road_edge (to_node);
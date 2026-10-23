package com.khanyi.java.gautenglogistics.ingest;

import com.khanyi.java.gautenglogistics.geo.Coordinate;
import de.topobyte.osm4j.core.access.OsmIterator;
import de.topobyte.osm4j.core.model.iface.EntityContainer;
import de.topobyte.osm4j.core.model.iface.EntityType;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.pbf.seq.PbfIterator;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Second pass over the OSM file: reads the coordinates of the nodes the drivable roads use.
 * Every other node in the file (buildings, trees, shops, footpaths) is skipped.
 */
public class NodeCoordinateReader {

    public Map<Long, Coordinate> read(Path osmFile, Set<Long> neededNodeIds) throws IOException {
        Map<Long, Coordinate> coordinates = new HashMap<>(neededNodeIds.size() * 4 / 3 + 1);
        try (InputStream in = new BufferedInputStream(Files.newInputStream(osmFile))) {
            OsmIterator iterator = new PbfIterator(in, false);
            for (EntityContainer container : iterator) {
                if (container.getType() != EntityType.Node) {
                    continue;
                }
                OsmNode node = (OsmNode) container.getEntity();
                if (neededNodeIds.contains(node.getId())) {
                    coordinates.put(node.getId(), new Coordinate(node.getLatitude(), node.getLongitude()));
                }
            }
        }
        return coordinates;
    }
}

package com.khanyi.java.gautenglogistics.ingest;

import de.topobyte.osm4j.core.access.OsmIterator;
import de.topobyte.osm4j.core.model.iface.EntityContainer;
import de.topobyte.osm4j.core.model.iface.EntityType;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.core.model.util.OsmModelUtil;
import de.topobyte.osm4j.pbf.seq.PbfIterator;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * First pass over the OSM file: collects the ways that are drivable roads.
 * Nodes and relations are skipped; their coordinates are read in a second pass,
 * once we know which nodes the roads actually use.
 */
public class DrivableWayReader {

    public List<DrivableWay> read(Path osmFile) throws IOException {
        List<DrivableWay> ways = new ArrayList<>();
        try (InputStream in = new BufferedInputStream(Files.newInputStream(osmFile))) {
            OsmIterator iterator = new PbfIterator(in, false); // false: skip metadata (authors, timestamps)
            for (EntityContainer container : iterator) {
                if (container.getType() == EntityType.Way) {
                    toDrivableWay((OsmWay) container.getEntity()).ifPresent(ways::add);
                }
            }
        }
        return ways;
    }

    static Optional<DrivableWay> toDrivableWay(OsmWay way) {
        if (way.getNumberOfNodes() < 2) {
            return Optional.empty();
        }
        Map<String, String> tags = OsmModelUtil.getTagsAsMap(way);
        Optional<RoadClass> roadClass = RoadTags.roadClass(tags);
        if (roadClass.isEmpty()) {
            return Optional.empty();
        }
        Optional<Direction> direction = RoadTags.direction(tags, roadClass.get());
        if (direction.isEmpty()) {
            return Optional.empty();
        }

        long[] nodeIds = new long[way.getNumberOfNodes()];
        for (int i = 0; i < nodeIds.length; i++) {
            nodeIds[i] = way.getNodeId(i);
        }
        // Store oneway=-1 roads reversed, so every one-way road runs first node -> last node
        if (direction.get() == Direction.BACKWARD) {
            reverse(nodeIds);
        }

        double speedKmh = RoadTags.speedKmh(tags, roadClass.get());
        boolean oneWay = direction.get() != Direction.TWO_WAY;
        return Optional.of(new DrivableWay(way.getId(), nodeIds, roadClass.get(), speedKmh, oneWay));
    }

    private static void reverse(long[] values) {
        for (int i = 0, j = values.length - 1; i < j; i++, j--) {
            long tmp = values[i];
            values[i] = values[j];
            values[j] = tmp;
        }
    }
}

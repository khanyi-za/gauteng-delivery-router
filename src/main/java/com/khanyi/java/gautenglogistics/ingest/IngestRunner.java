package com.khanyi.java.gautenglogistics.ingest;

import com.khanyi.java.gautenglogistics.geo.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Imports the road map when the app starts, if gautenglogistics.ingest.enabled=true:
 * reads the OSM file, cuts the roads into pieces between intersections, keeps the biggest
 * part where you can drive everywhere and back, and saves it to the database.
 */
@Component
@ConditionalOnBooleanProperty("gautenglogistics.ingest.enabled")
class IngestRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(IngestRunner.class);

    private final IngestProperties properties;
    private final RoadNetworkWriter writer;

    IngestRunner(IngestProperties properties, RoadNetworkWriter writer) {
        this.properties = properties;
        this.writer = writer;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Ingesting road network from {}", properties.osmFile().toAbsolutePath());
        long start = System.nanoTime();

        List<DrivableWay> ways = new DrivableWayReader().read(properties.osmFile());
        log.info("Pass 1: {} drivable ways ({} ms)", ways.size(), elapsedMs(start));

        Map<Long, Integer> nodeUses = EdgeBuilder.countNodeUses(ways);
        Map<Long, Coordinate> coordinates = new NodeCoordinateReader().read(properties.osmFile(), nodeUses.keySet());
        log.info("Pass 2: coordinates for {} of {} road nodes ({} ms)",
                coordinates.size(), nodeUses.size(), elapsedMs(start));

        List<RawEdge> edges = EdgeBuilder.buildEdges(ways, nodeUses, coordinates);
        log.info("Built {} directed edges ({} ms)", edges.size(), elapsedMs(start));

        List<RawEdge> kept = LargestComponentFilter.keepLargestComponent(edges);
        double keptKm = kept.stream().mapToDouble(RawEdge::lengthM).sum() / 1000;
        log.info("Largest strongly connected component: {} of {} edges, {} km of lanes ({} ms)",
                kept.size(), edges.size(), Math.round(keptKm), elapsedMs(start));

        int nodes = writer.replaceNetwork(kept, coordinates);
        log.info("Saved {} nodes and {} edges to the database ({} ms)", nodes, kept.size(), elapsedMs(start));
    }

    private static long elapsedMs(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }
}

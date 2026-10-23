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
 * Runs the road network ingester at startup when gautenglogistics.ingest.enabled=true.
 * For now it builds the edges in memory and logs what it found.
 */
@Component
@ConditionalOnBooleanProperty("gautenglogistics.ingest.enabled")
class IngestRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(IngestRunner.class);

    private final IngestProperties properties;

    IngestRunner(IngestProperties properties) {
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Ingesting road network from {}", properties.osmFile().toAbsolutePath());
        long start = System.nanoTime();

        List<DrivableWay> ways = new DrivableWayReader().read(properties.osmFile());
        log.info("Pass 1: {} drivable ways ({} ms)", ways.size(), elapsedMs(start));

        Map<Long, Integer> nodeUses = EdgeBuilder.countNodeUses(ways);
        long vertices = nodeUses.values().stream().filter(count -> count >= 2).count();

        Map<Long, Coordinate> coordinates = new NodeCoordinateReader().read(properties.osmFile(), nodeUses.keySet());
        log.info("Pass 2: coordinates for {} of {} road nodes ({} ms)",
                coordinates.size(), nodeUses.size(), elapsedMs(start));

        List<RawEdge> edges = EdgeBuilder.buildEdges(ways, nodeUses, coordinates);
        double totalKm = edges.stream().mapToDouble(RawEdge::lengthM).sum() / 1000;
        log.info("Built {} directed edges between {} intersections, {} km of lanes ({} ms)",
                edges.size(), vertices, Math.round(totalKm), elapsedMs(start));

        Runtime runtime = Runtime.getRuntime();
        log.info("Heap in use: {} MB", (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024));
    }

    private static long elapsedMs(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }
}

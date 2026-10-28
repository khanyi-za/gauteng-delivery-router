package com.khanyi.java.gautenglogistics.graph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Loads the road map into memory once, when the app starts, and shares it with the rest of the app,
 * together with the grid index for finding the nearest road to a GPS point.
 */
@Configuration(proxyBeanMethods = false)
class RoadGraphConfiguration {

    private static final Logger log = LoggerFactory.getLogger(RoadGraphConfiguration.class);

    /** A GPS point further than this from every road gets "no road nearby". */
    private static final double MAX_SNAP_DISTANCE_M = 2_000;

    @Bean
    RoadGraph roadGraph(RoadGraphLoader loader) {
        long start = System.nanoTime();
        RoadGraph graph = loader.load();
        long ms = (System.nanoTime() - start) / 1_000_000;
        if (graph.nodeCount() == 0) {
            log.warn("The road map is empty. Import it with --gautenglogistics.ingest.enabled=true, then restart");
        } else {
            log.info("Loaded road graph: {} intersections, {} edges ({} ms)", graph.nodeCount(), graph.edgeCount(), ms);
        }
        return graph;
    }

    @Bean
    GridIndex gridIndex(RoadGraph roadGraph) {
        return new GridIndex(roadGraph, MAX_SNAP_DISTANCE_M);
    }
}

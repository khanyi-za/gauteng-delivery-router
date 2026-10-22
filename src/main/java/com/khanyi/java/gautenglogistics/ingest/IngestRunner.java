package com.khanyi.java.gautenglogistics.ingest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Runs the road network ingester at startup when gautenglogistics.ingest.enabled=true.
 * For now it only reads the drivable roads and logs what it found.
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
        log.info("Reading drivable roads from {}", properties.osmFile().toAbsolutePath());
        long start = System.nanoTime();

        List<DrivableWay> ways = new DrivableWayReader().read(properties.osmFile());

        long nodeRefs = ways.stream().mapToLong(way -> way.nodeIds().length).sum();
        long oneWay = ways.stream().filter(DrivableWay::oneWay).count();
        Map<RoadClass, Integer> byClass = new EnumMap<>(RoadClass.class);
        for (DrivableWay way : ways) {
            byClass.merge(way.roadClass(), 1, Integer::sum);
        }

        log.info("Read {} drivable ways ({} one-way, {} node references) in {} ms",
                ways.size(), oneWay, nodeRefs, (System.nanoTime() - start) / 1_000_000);
        byClass.forEach((roadClass, count) -> log.info("  {}: {}", roadClass.tag(), count));
    }
}

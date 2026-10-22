package com.khanyi.java.gautenglogistics.ingest;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

/**
 * Settings under gautenglogistics.ingest in application.properties.
 *
 * @param osmFile the OpenStreetMap PBF file to read the road network from
 * @param enabled whether to run the ingester when the application starts
 */
@ConfigurationProperties("gautenglogistics.ingest")
public record IngestProperties(Path osmFile, boolean enabled) {
}
package com.khanyi.java.gautenglogistics.ingest;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class RoadTagsTest {

    @Test
    void residentialRoadIsDrivable() {
        assertThat(RoadTags.roadClass(Map.of("highway", "residential"))).contains(RoadClass.RESIDENTIAL);
    }

    @Test
    void footpathsAndPrivateRoadsAreNotDrivable() {
        assertThat(RoadTags.roadClass(Map.of("highway", "footway"))).isEmpty();
        assertThat(RoadTags.roadClass(Map.of("highway", "residential", "access", "private"))).isEmpty();
        assertThat(RoadTags.roadClass(Map.of("highway", "service", "service", "parking_aisle"))).isEmpty();
        assertThat(RoadTags.roadClass(Map.of("highway", "pedestrian", "area", "yes"))).isEmpty();
    }

    @Test
    void readsOnewayTags() {
        assertThat(RoadTags.direction(Map.of("oneway", "yes"), RoadClass.RESIDENTIAL)).contains(Direction.FORWARD);
        assertThat(RoadTags.direction(Map.of("oneway", "-1"), RoadClass.RESIDENTIAL)).contains(Direction.BACKWARD);
        assertThat(RoadTags.direction(Map.of("oneway", "no"), RoadClass.MOTORWAY)).contains(Direction.TWO_WAY);
        assertThat(RoadTags.direction(Map.of("oneway", "reversible"), RoadClass.PRIMARY)).isEmpty();
    }

    @Test
    void motorwaysAndRoundaboutsAreOneWayByDefault() {
        assertThat(RoadTags.direction(Map.of(), RoadClass.MOTORWAY)).contains(Direction.FORWARD);
        assertThat(RoadTags.direction(Map.of("junction", "roundabout"), RoadClass.SECONDARY)).contains(Direction.FORWARD);
        assertThat(RoadTags.direction(Map.of(), RoadClass.RESIDENTIAL)).contains(Direction.TWO_WAY);
    }

    @Test
    void parsesMaxspeedFormats() {
        assertThat(RoadTags.parseMaxSpeedKmh("80")).hasValue(80);
        assertThat(RoadTags.parseMaxSpeedKmh("ZA:urban")).hasValue(60);
        assertThat(RoadTags.parseMaxSpeedKmh("60;80")).hasValue(60);
        assertThat(RoadTags.parseMaxSpeedKmh("30 mph").getAsDouble()).isCloseTo(48.28, within(0.01));
        assertThat(RoadTags.parseMaxSpeedKmh("none")).isEmpty();
        assertThat(RoadTags.parseMaxSpeedKmh("signals")).isEmpty();
        assertThat(RoadTags.parseMaxSpeedKmh(null)).isEmpty();
    }

    @Test
    void speedFallsBackToRoadClassDefaultAndIsCapped() {
        assertThat(RoadTags.speedKmh(Map.of(), RoadClass.RESIDENTIAL)).isEqualTo(60);
        assertThat(RoadTags.speedKmh(Map.of("maxspeed", "40"), RoadClass.RESIDENTIAL)).isEqualTo(40);
        assertThat(RoadTags.speedKmh(Map.of("maxspeed", "160"), RoadClass.MOTORWAY)).isEqualTo(RoadClass.MAX_SPEED_KMH);
    }
}

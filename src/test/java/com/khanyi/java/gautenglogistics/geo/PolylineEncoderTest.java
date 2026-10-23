package com.khanyi.java.gautenglogistics.geo;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PolylineEncoderTest {

    @Test
    void encodesTheReferenceExampleFromTheFormatSpecification() {
        List<Coordinate> points = List.of(
                new Coordinate(38.5, -120.2),
                new Coordinate(40.7, -120.95),
                new Coordinate(43.252, -126.453));

        assertThat(PolylineEncoder.encode(points)).isEqualTo("_p~iF~ps|U_ulLnnqC_mqNvxq`@");
    }

    @Test
    void encodesNegativeValuesLikeTheSpecification() {
        // The specification's worked example for a negative number: -179.9832104 encodes as "`~oia@".
        // Gauteng's latitudes are all negative, so the sign handling matters. A longitude of 0 encodes as "?".
        assertThat(PolylineEncoder.encode(List.of(new Coordinate(-179.9832104, 0)))).isEqualTo("`~oia@?");
    }

    @Test
    void emptyListGivesEmptyString() {
        assertThat(PolylineEncoder.encode(List.of())).isEmpty();
    }
}

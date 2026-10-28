package com.khanyi.java.gautenglogistics.geo;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PolylineDecoderTest {

    @Test
    void decodesTheReferenceExampleFromTheFormatSpecification() {
        assertThat(PolylineDecoder.decode("_p~iF~ps|U_ulLnnqC_mqNvxq`@")).containsExactly(
                new Coordinate(38.5, -120.2),
                new Coordinate(40.7, -120.95),
                new Coordinate(43.252, -126.453));
    }

    @Test
    void givesBackWhatTheEncoderWrote() {
        // Gauteng points: negative latitudes, positive longitudes, already at 5 decimal places
        List<Coordinate> points = List.of(
                new Coordinate(-26.20410, 28.04730),
                new Coordinate(-26.10760, 28.05670),
                new Coordinate(-25.74790, 28.22930));

        assertThat(PolylineDecoder.decode(PolylineEncoder.encode(points))).isEqualTo(points);
    }

    @Test
    void emptyStringGivesNoPoints() {
        assertThat(PolylineDecoder.decode("")).isEmpty();
    }

    @Test
    void refusesTextThatStopsHalfwayThroughANumber() {
        // "_" has the "more follows" bit set, but nothing follows
        assertThatThrownBy(() -> PolylineDecoder.decode("_"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

package com.khanyi.java.gautenglogistics.geo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class HaversineTest {

    @Test
    void samePointIsZeroMetresApart() {
        Coordinate sandton = new Coordinate(-26.1076, 28.0567);
        assertThat(Haversine.distanceMetres(sandton, sandton)).isZero();
    }

    @Test
    void oneDegreeOfLatitudeIsAbout111Kilometres() {
        double metres = Haversine.distanceMetres(new Coordinate(-26, 28), new Coordinate(-25, 28));
        assertThat(metres).isCloseTo(111_195, within(10.0));
    }

    @Test
    void johannesburgToPretoriaIsAbout53Kilometres() {
        Coordinate johannesburg = new Coordinate(-26.2041, 28.0473);
        Coordinate pretoria = new Coordinate(-25.7479, 28.2293);
        assertThat(Haversine.distanceMetres(johannesburg, pretoria)).isCloseTo(53_600, within(500.0));
    }

    @Test
    void distanceIsTheSameInBothDirections() {
        Coordinate a = new Coordinate(-26.2041, 28.0473);
        Coordinate b = new Coordinate(-26.2678, 27.8585);
        assertThat(Haversine.distanceMetres(a, b)).isEqualTo(Haversine.distanceMetres(b, a));
    }
}

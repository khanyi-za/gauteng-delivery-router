package com.khanyi.java.gautenglogistics.routing;

import com.khanyi.java.gautenglogistics.geo.Coordinate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** A drive east along three points: the first part takes 10 seconds, the second 30. */
class TimedPathTest {

    private static final Coordinate A = new Coordinate(-26.10, 28.00);
    private static final Coordinate B = new Coordinate(-26.10, 28.01);
    private static final Coordinate C = new Coordinate(-26.10, 28.03);

    private final TimedPath path = new TimedPath(List.of(A, B, C), List.of(0.0, 10.0, 40.0));

    @Test
    void theDurationIsTheTimeTheLastPointIsReached() {
        assertThat(path.durationS()).isEqualTo(40);
    }

    @Test
    void isAtAPointAtTheTimeItIsReached() {
        assertThat(path.positionAt(0)).isEqualTo(A);
        assertThat(path.positionAt(10)).isEqualTo(B);
        assertThat(path.positionAt(40)).isEqualTo(C);
    }

    @Test
    void isPartWayAlongBetweenPoints() {
        Coordinate quarterOfTheWayFromBToC = path.positionAt(17.5);

        assertThat(quarterOfTheWayFromBToC.lat()).isCloseTo(-26.10, within(1e-9));
        assertThat(quarterOfTheWayFromBToC.lon()).isCloseTo(28.015, within(1e-9));
    }

    @Test
    void staysAtTheStartBeforeLeavingAndAtTheEndAfterArriving() {
        assertThat(path.positionAt(-5)).isEqualTo(A);
        assertThat(path.positionAt(100)).isEqualTo(C);
    }

    @Test
    void aOnePointPathIsAlwaysThere() {
        TimedPath standingStill = new TimedPath(List.of(A), List.of(0.0));

        assertThat(standingStill.durationS()).isZero();
        assertThat(standingStill.positionAt(30)).isEqualTo(A);
    }

    @Test
    void everyPointNeedsATime() {
        assertThatThrownBy(() -> new TimedPath(List.of(A, B), List.of(0.0)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TimedPath(List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

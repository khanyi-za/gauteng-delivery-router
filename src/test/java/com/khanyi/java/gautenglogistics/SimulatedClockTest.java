package com.khanyi.java.gautenglogistics;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class SimulatedClockTest {

    private static final Instant REAL_START = Instant.parse("2026-11-02T08:00:00Z");
    private static final Instant SIMULATED_START = Instant.parse("2026-11-05T06:00:00Z");

    @Test
    void atSpeed60OneRealSecondIsOneSimulatedMinute() {
        Clock realClock = mock(Clock.class);
        // The first answer is read when the simulated clock is created, the next ones later
        given(realClock.instant()).willReturn(REAL_START, REAL_START, REAL_START.plusSeconds(10));
        SimulatedClock clock = new SimulatedClock(realClock, SIMULATED_START, 60);

        assertThat(clock.instant()).isEqualTo(SIMULATED_START);
        assertThat(clock.instant()).isEqualTo(SIMULATED_START.plusSeconds(600)); // 10 real seconds later
    }

    @Test
    void atSpeed1ItKeepsRealTime() {
        Clock realClock = mock(Clock.class);
        given(realClock.instant()).willReturn(REAL_START, REAL_START.plusMillis(1_500));
        SimulatedClock clock = new SimulatedClock(realClock, SIMULATED_START, 1);

        assertThat(clock.instant()).isEqualTo(SIMULATED_START.plusMillis(1_500));
    }
}

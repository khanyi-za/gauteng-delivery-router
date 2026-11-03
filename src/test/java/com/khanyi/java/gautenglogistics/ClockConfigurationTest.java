package com.khanyi.java.gautenglogistics;

import com.khanyi.java.gautenglogistics.repository.DeliveryOrderRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class ClockConfigurationTest {

    private final DeliveryOrderRepository orderRepository = mock(DeliveryOrderRepository.class);

    @Test
    void startsAtTheRealTimeWhenNothingHasBeenSimulatedYet() {
        given(orderRepository.findLatestTime()).willReturn(Optional.empty());

        Clock clock = new ClockConfiguration().clock(orderRepository, 60);

        assertThat(clock.instant()).isCloseTo(Instant.now(), within(Duration.ofMinutes(1)));
    }

    @Test
    void carriesOnFromWhereAnEarlierRunGotTo() {
        Instant twoDaysAhead = Instant.now().plus(Duration.ofDays(2));
        given(orderRepository.findLatestTime()).willReturn(Optional.of(twoDaysAhead));

        Clock clock = new ClockConfiguration().clock(orderRepository, 60);

        assertThat(clock.instant()).isCloseTo(twoDaysAhead, within(Duration.ofMinutes(1)));
    }

    @Test
    void ignoresTimesThatAreAlreadyInThePast() {
        given(orderRepository.findLatestTime()).willReturn(Optional.of(Instant.parse("2020-01-01T00:00:00Z")));

        Clock clock = new ClockConfiguration().clock(orderRepository, 60);

        assertThat(clock.instant()).isCloseTo(Instant.now(), within(Duration.ofMinutes(1)));
    }
}

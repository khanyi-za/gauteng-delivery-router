package com.khanyi.java.gautenglogistics.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.Clock;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/** Tests only the web layer, with a pretend clock. */
@WebMvcTest(ClockController.class)
class ClockControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private Clock clock;

    @Test
    void showsTheSimulatedTimeAndHowFastItRuns() {
        given(clock.instant()).willReturn(Instant.parse("2026-11-02T10:00:00Z"));

        assertThat(mvc.get().uri("/clock"))
                .hasStatusOk()
                .bodyJson()
                .isStrictlyEqualTo("""
                        {"now": "2026-11-02T10:00:00Z", "speed": 60.0}""");
    }
}

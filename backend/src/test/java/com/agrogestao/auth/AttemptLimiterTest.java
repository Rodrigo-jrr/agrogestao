package com.agrogestao.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class AttemptLimiterTest {

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-09-30T12:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @Test
    void blocksAfterMaxAttemptsAndReleasesWhenWindowPasses() {
        MutableClock clock = new MutableClock();
        AttemptLimiter limiter = new AttemptLimiter(3, Duration.ofMinutes(15), clock);

        limiter.record("k");
        clock.advance(Duration.ofMinutes(5));
        limiter.record("k");
        assertThat(limiter.blockedFor("k")).isEmpty();
        limiter.record("k");
        assertThat(limiter.blockedFor("k")).contains(Duration.ofMinutes(10));

        clock.advance(Duration.ofMinutes(10).plusSeconds(1));
        assertThat(limiter.blockedFor("k")).isEmpty();
    }

    @Test
    void resetClearsKey() {
        AttemptLimiter limiter = new AttemptLimiter(1, Duration.ofMinutes(15), new MutableClock());
        limiter.record("k");
        assertThat(limiter.blockedFor("k")).isPresent();
        limiter.reset("k");
        assertThat(limiter.blockedFor("k")).isEmpty();
    }
}

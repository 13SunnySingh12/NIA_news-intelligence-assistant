package com.nia.common;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The limiter is a long-lived singleton, so its bookkeeping must both enforce
 * the limit and stay bounded: it used to keep one entry per (user, action) pair
 * for the lifetime of the process, with nothing ever removing them.
 */
class RateLimiterTest {

    /** A clock the test can move forward by whole hours. */
    private static final class MovableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }

        void advanceHours(long hours) { now = now.plus(Duration.ofHours(hours)); }
    }

    @Test
    void allowsUpToTheLimitThenDenies() {
        RateLimiter limiter = new RateLimiter();
        String user = UUID.randomUUID().toString();
        for (int i = 0; i < 5; i++) {
            assertThat(limiter.tryAcquire(user, "test", 5))
                    .as("request %d of 5", i + 1)
                    .isTrue();
        }
        assertThat(limiter.tryAcquire(user, "test", 5)).isFalse();
        assertThat(limiter.tryAcquire(user, "test", 5)).isFalse();
    }

    @Test
    void countsEachActionSeparately() {
        RateLimiter limiter = new RateLimiter();
        String user = UUID.randomUUID().toString();
        assertThat(limiter.tryAcquire(user, "action_a", 1)).isTrue();
        assertThat(limiter.tryAcquire(user, "action_a", 1)).isFalse();
        assertThat(limiter.tryAcquire(user, "action_b", 1)).isTrue();
    }

    @Test
    void countsEachUserSeparately() {
        RateLimiter limiter = new RateLimiter();
        assertThat(limiter.tryAcquire("user-one", "test", 1)).isTrue();
        assertThat(limiter.tryAcquire("user-one", "test", 1)).isFalse();
        assertThat(limiter.tryAcquire("user-two", "test", 1)).isTrue();
    }

    @Test
    void budgetResetsWhenTheHourRolls() {
        MovableClock clock = new MovableClock();
        RateLimiter limiter = new RateLimiter(clock);
        String user = UUID.randomUUID().toString();

        assertThat(limiter.tryAcquire(user, "test", 1)).isTrue();
        assertThat(limiter.tryAcquire(user, "test", 1)).isFalse();

        clock.advanceHours(1);
        assertThat(limiter.tryAcquire(user, "test", 1))
                .as("a new hour starts a fresh window")
                .isTrue();
    }

    /**
     * Every distinct caller used to leave an entry behind permanently. Fill the
     * map past the sweep threshold in one hour, roll the clock, and assert the
     * now-unreachable windows are actually released.
     */
    @Test
    void releasesWindowsFromEarlierHoursInsteadOfGrowingForever() {
        MovableClock clock = new MovableClock();
        RateLimiter limiter = new RateLimiter(clock);

        for (int i = 0; i < 12_000; i++) {
            limiter.tryAcquire("user-" + i, "test", 10);
        }
        int afterFirstHour = limiter.trackedWindows();
        assertThat(afterFirstHour).isGreaterThan(10_000);

        clock.advanceHours(1);
        // One request in the new hour triggers the sweep of last hour's windows.
        limiter.tryAcquire("someone-new", "test", 10);

        assertThat(limiter.trackedWindows())
                .as("windows from the previous hour must not be retained")
                .isLessThan(afterFirstHour)
                .isLessThan(100);

        // Still enforcing correctly after a sweep.
        String user = UUID.randomUUID().toString();
        assertThat(limiter.tryAcquire(user, "test", 1)).isTrue();
        assertThat(limiter.tryAcquire(user, "test", 1)).isFalse();
    }
}

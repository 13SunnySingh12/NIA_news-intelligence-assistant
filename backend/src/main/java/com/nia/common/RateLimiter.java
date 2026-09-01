package com.nia.common;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tiny in-memory, per-user, fixed-window (1 hour) rate limiter.
 * Enough for a single-instance deployment; NIA deliberately avoids Redis.
 * If scaled to multiple instances, move this to a small Postgres table.
 */
@Component
public class RateLimiter {

    private record Window(long hourEpoch, AtomicInteger count) {}

    /**
     * Once more than this many windows are tracked, the ones belonging to an
     * earlier hour are dropped. Without that sweep the map keeps an entry per
     * (user, action) pair for the lifetime of the process — every user who ever
     * made a request stays resident. The threshold sits well above any realistic
     * number of users active within one hour, so the sweep does no work during
     * normal operation.
     */
    private static final int SWEEP_THRESHOLD = 10_000;

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;

    public RateLimiter() {
        this(Clock.systemUTC());
    }

    /** Test seam: lets a test advance the hour without waiting an hour. */
    RateLimiter(Clock clock) {
        this.clock = clock;
    }

    /** Returns true if the action is allowed, incrementing the counter. */
    public boolean tryAcquire(String userId, String action, int limitPerHour) {
        long currentHour = clock.instant().getEpochSecond() / 3600;
        String key = userId + ":" + action;

        if (windows.size() > SWEEP_THRESHOLD) {
            // A window from an earlier hour can never allow or deny anything again.
            windows.values().removeIf(window -> window.hourEpoch() != currentHour);
        }

        Window window = windows.compute(key, (k, existing) -> {
            if (existing == null || existing.hourEpoch() != currentHour) {
                return new Window(currentHour, new AtomicInteger(0));
            }
            return existing;
        });

        return window.count().incrementAndGet() <= limitPerHour;
    }

    /** How many windows are currently held. Exposed so the sweep can be tested. */
    int trackedWindows() {
        return windows.size();
    }
}

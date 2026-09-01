package com.nia.common;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The limiter is a singleton shared by every request thread, and the sweep added
 * to bound its memory mutates the same map that callers are incrementing. These
 * tests pin that the sweep cannot over-admit or lose counts under contention.
 */
class RateLimiterConcurrencyTest {

    @Test
    void neverAdmitsMoreThanTheLimitUnderParallelLoad() throws Exception {
        RateLimiter limiter = new RateLimiter();
        int limit = 50;
        int threads = 16;
        int attemptsPerThread = 20;   // 320 attempts against a limit of 50

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger allowed = new AtomicInteger();

        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < attemptsPerThread; i++) {
                        if (limiter.tryAcquire("shared-user", "action", limit)) {
                            allowed.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        assertThat(allowed.get())
                .as("a shared counter must not let extra requests through under contention")
                .isEqualTo(limit);
    }

    @Test
    void sweepUnderLoadDoesNotCorruptOtherUsersCounters() throws Exception {
        RateLimiter limiter = new RateLimiter();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger allowedForTracked = new AtomicInteger();

        // One tracked user competing with heavy churn from many one-off users,
        // which is what pushes the map past the sweep threshold.
        pool.submit(() -> {
            try {
                start.await();
                for (int i = 0; i < 10; i++) {
                    if (limiter.tryAcquire("tracked-user", "action", 10)) {
                        allowedForTracked.incrementAndGet();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        for (int t = 0; t < 7; t++) {
            final int offset = t;
            pool.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < 3000; i++) {
                        limiter.tryAcquire("churn-" + offset + "-" + i, "action", 5);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        assertThat(allowedForTracked.get())
                .as("the tracked user's 10 requests must all be admitted despite concurrent sweeping")
                .isEqualTo(10);
        assertThat(limiter.tryAcquire("tracked-user", "action", 10))
                .as("the 11th must still be denied, so the sweep did not reset a live counter")
                .isFalse();
    }
}

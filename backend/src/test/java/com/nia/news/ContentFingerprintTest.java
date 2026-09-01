package com.nia.news;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ContentFingerprintTest {

    private final Instant day1 = Instant.parse("2026-08-30T10:00:00Z");
    private final Instant day1Later = Instant.parse("2026-08-30T22:00:00Z");
    private final Instant day2 = Instant.parse("2026-08-31T10:00:00Z");

    @Test
    void sameTitleSameDayIsSameFingerprint() {
        // Ignores punctuation/case/whitespace and the exact time within the day.
        assertThat(ContentFingerprint.of("Big News: Something Happened!", day1))
                .isEqualTo(ContentFingerprint.of("big news something happened", day1Later));
    }

    @Test
    void sameTitleDifferentDayIsDifferentFingerprint() {
        assertThat(ContentFingerprint.of("Recurring headline", day1))
                .isNotEqualTo(ContentFingerprint.of("Recurring headline", day2));
    }

    @Test
    void differentTitleIsDifferentFingerprint() {
        assertThat(ContentFingerprint.of("Story one", day1))
                .isNotEqualTo(ContentFingerprint.of("A completely different story", day1));
    }
}

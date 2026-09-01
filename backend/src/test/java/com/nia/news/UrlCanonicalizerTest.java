package com.nia.news;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UrlCanonicalizerTest {

    @Test
    void stripsTrackingParamsAndTrailingSlash() {
        String canonical = UrlCanonicalizer.canonicalize(
                "https://Example.com/News/Story/?utm_source=twitter&fbclid=abc");
        assertThat(canonical).isEqualTo("https://example.com/News/Story");
    }

    @Test
    void keepsMeaningfulQueryParams() {
        String canonical = UrlCanonicalizer.canonicalize("https://example.com/a?id=42&utm_medium=x");
        assertThat(canonical).isEqualTo("https://example.com/a?id=42");
    }

    @Test
    void handlesBlankAndMalformedInput() {
        assertThat(UrlCanonicalizer.canonicalize(null)).isEmpty();
        assertThat(UrlCanonicalizer.canonicalize("   ")).isEmpty();
    }
}

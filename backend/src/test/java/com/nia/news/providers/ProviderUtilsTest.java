package com.nia.news.providers;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Feeds hand us HTML, not plain text. Stripping tags without decoding entities
 * put a literal "&nbsp;&nbsp;" in front of readers on roughly half the corpus,
 * so these cases are taken from the strings that actually shipped.
 */
class ProviderUtilsTest {

    @Test
    void decodesTheNbspThatLeakedIntoArticleCards() {
        String raw = "<a href=\"https://news.google.com/x\">World Best Times Fall on Final Day</a>"
                + "&nbsp;&nbsp;<font color=\"#6f6f6f\">Rowing News</font>";
        assertThat(ProviderUtils.stripHtml(raw))
                .isEqualTo("World Best Times Fall on Final Day Rowing News")
                .doesNotContain("&nbsp;");
    }

    /**
     * Asserted as escaped code points on purpose. Comparing against a literal
     * "’" would still pass if the compiler mangled the source and the test
     * encoding identically, so the literal proves nothing about what a reader
     * actually receives.
     */
    @Test
    void decodedCharactersAreTheCorrectCodePoints() {
        assertThat(ProviderUtils.stripHtml("a&rsquo;b")).isEqualTo("a\u2019b");
        assertThat(ProviderUtils.stripHtml("a&pound;b")).isEqualTo("a\u00a3b");
        assertThat(ProviderUtils.stripHtml("a&hellip;b")).isEqualTo("a\u2026b");
        assertThat(ProviderUtils.stripHtml("a&eacute;b")).isEqualTo("a\u00e9b");
        assertThat(ProviderUtils.stripHtml("a&euro;b")).isEqualTo("a\u20acb");
        // &nbsp; must end up as a plain space, not a raw U+00A0 that renders oddly
        // and is invisible to \s-based trimming downstream.
        assertThat(ProviderUtils.stripHtml("a&nbsp;b")).isEqualTo("a b");
        assertThat(ProviderUtils.stripHtml("a&nbsp;b")).doesNotContain("\u00a0");
    }

    @Test
    void decodesCommonNamedEntities() {
        assertThat(ProviderUtils.stripHtml("Fish &amp; chips")).isEqualTo("Fish & chips");
        assertThat(ProviderUtils.stripHtml("India&rsquo;s economy")).isEqualTo("India’s economy");
        assertThat(ProviderUtils.stripHtml("Cost rose &pound;20m")).isEqualTo("Cost rose £20m");
        assertThat(ProviderUtils.stripHtml("Say &quot;hello&quot;")).isEqualTo("Say \"hello\"");
        assertThat(ProviderUtils.stripHtml("More&hellip;")).isEqualTo("More…");
    }

    @Test
    void decodesNumericEntitiesInBothDecimalAndHex() {
        assertThat(ProviderUtils.stripHtml("It&#39;s here")).isEqualTo("It's here");
        assertThat(ProviderUtils.stripHtml("It&#x27;s here")).isEqualTo("It's here");
        assertThat(ProviderUtils.stripHtml("caf&#233;")).isEqualTo("café");
    }

    @Test
    void leavesUnknownOrMalformedEntitiesAlone() {
        assertThat(ProviderUtils.stripHtml("100% &notarealentity; ok"))
                .isEqualTo("100% &notarealentity; ok");
        assertThat(ProviderUtils.stripHtml("bad &#99999999999; entity"))
                .isEqualTo("bad &#99999999999; entity");
    }

    @Test
    void decodingCannotReintroduceMarkup() {
        // The '<' arrives encoded; decoding happens after tags are stripped, so the
        // result must be inert text rather than a tag that survived the strip.
        String out = ProviderUtils.stripHtml("safe &lt;script&gt;alert(1)&lt;/script&gt; text");
        assertThat(out).isEqualTo("safe <script>alert(1)</script> text");
        assertThat(ProviderUtils.stripHtml("<script>alert(1)</script>real text"))
                .isEqualTo("alert(1) real text");
    }

    @Test
    void stripsTagsAndCollapsesWhitespaceAsBefore() {
        assertThat(ProviderUtils.stripHtml("<p>one</p>   <p>two</p>")).isEqualTo("one two");
        assertThat(ProviderUtils.stripHtml("   ")).isNull();
        assertThat(ProviderUtils.stripHtml("<br/>")).isNull();
        assertThat(ProviderUtils.stripHtml(null)).isNull();
    }

    @Test
    void dropsATagLeftDanglingByTruncation() {
        // Guardian truncates bodyText to a length limit before it is sanitized, so
        // the cut can land inside a tag and leave no closing '>'.
        assertThat(ProviderUtils.stripHtml("Some body text<p")).isEqualTo("Some body text");
        assertThat(ProviderUtils.stripHtml("Story continues <a href=\"http")).isEqualTo("Story continues");
        assertThat(ProviderUtils.stripHtml("clean text")).isEqualTo("clean text");
        // A bare '<' in ordinary prose must not eat the rest of the sentence when
        // it is followed by a closing bracket later.
        assertThat(ProviderUtils.stripHtml("5 < 10 is true")).isEqualTo("5 < 10 is true");
    }

    @Test
    void truncateAndTextHelpersStillBehave() {
        assertThat(ProviderUtils.truncate("abcdef", 3)).isEqualTo("abc");
        assertThat(ProviderUtils.truncate("ab", 5)).isEqualTo("ab");
        assertThat(ProviderUtils.truncate(null, 5)).isNull();
    }
}

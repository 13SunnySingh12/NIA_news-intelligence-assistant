package com.nia.news;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cases taken from articles that were really misfiled in the live database, so
 * the validator is measured against the mistakes it exists to prevent.
 */
class CategoryValidatorTest {

    private final CategoryValidator validator = new CategoryValidator();

    private NormalizedArticle article(String title, String description, String source) {
        return NormalizedArticle.create(title, description, "https://example.com/a", null,
                source, null, "india", "en", "in", Instant.now(), null, "test");
    }

    // ---- India: reject global filler that a provider returned for an India query ----

    @Test
    void rejectsArticleWithNoIndiaSignalAnywhere() {
        assertThat(validator.validate(
                article("Why I Stopped Writing Regex by Hand", "A developer essay.", "Currents"), "india"))
                .isEqualTo(CategoryValidator.Verdict.REJECT);
    }

    @Test
    void rejectsForeignLocalNews() {
        assertThat(validator.validate(
                article("Fatal crash leads to closures of Redwood Road in Salt Lake City", "", "Currents"), "india"))
                .isEqualTo(CategoryValidator.Verdict.REJECT);
    }

    @Test
    void rejectsStoryAboutAnotherCountry() {
        assertThat(validator.validate(
                article("Expert on Poland's pre-1939 ethnocide policy in Western Belarus", "", "Currents"), "india"))
                .isEqualTo(CategoryValidator.Verdict.REJECT);
    }

    // ---- India: keep genuine Indian news that never says the word "India" ----

    @Test
    void acceptsIndianStateNewsWithoutTheWordIndia() {
        assertThat(validator.validate(
                article("Karnataka High Court upholds Rs 15 lakh alimony", "", "indianexpress"), "india"))
                .isEqualTo(CategoryValidator.Verdict.ACCEPT);
    }

    @Test
    void acceptsIndianCityNewsFromAnyPublisher() {
        assertThat(validator.validate(
                article("Punjab Governor launches youth facilitation centre in Ludhiana", "", "Some Wire"), "india"))
                .isEqualTo(CategoryValidator.Verdict.ACCEPT);
    }

    @Test
    void acceptsIndianPublisherCoverage() {
        assertThat(validator.validate(
                article("Over 2 Crore Voters Excluded From Maharashtra's Draft Electoral Roll", "", "NDTV"), "india"))
                .isEqualTo(CategoryValidator.Verdict.ACCEPT);
    }

    // These are real stories that were being dropped: the publisher is plainly
    // Indian but was not in the curated source list, and with no description to
    // fall back on the validator saw no India evidence at all.

    @Test
    void acceptsAnIndianPublisherThatIsNotInTheCuratedList() {
        assertThat(validator.validate(
                article("August 2026 car sales: Tata widens lead over Mahindra", "", "Autocar India"), "india"))
                .isEqualTo(CategoryValidator.Verdict.ACCEPT);
    }

    @Test
    void acceptsAGovernmentIndianSource() {
        assertThat(validator.validate(
                article("PM participates in the 26th SCO Summit in Bishkek", "", "PM India"), "india"))
                .isEqualTo(CategoryValidator.Verdict.ACCEPT);
    }

    @Test
    void acceptsAnIndianStateNamedOnlyInThePublisher() {
        assertThat(validator.validate(
                article("Fishermen warned as swell surge continues", "", "Kerala Kaumudi"), "india"))
                .isEqualTo(CategoryValidator.Verdict.ACCEPT);
    }

    @Test
    void stillRejectsForeignPublishersWithNoIndiaSignal() {
        // The source check must not turn into "accept everything".
        assertThat(validator.validate(
                article("Fatal crash closes Redwood Road", "", "Salt Lake Tribune"), "india"))
                .isEqualTo(CategoryValidator.Verdict.REJECT);
        assertThat(validator.validate(
                article("Local council approves new budget", "", "BBC News"), "india"))
                .isEqualTo(CategoryValidator.Verdict.REJECT);
    }

    @Test
    void aWordLikeGoalInAPublisherNameIsNotTheStateOfGoa() {
        assertThat(validator.validate(
                article("Late winner seals derby", "", "Goal.com"), "india"))
                .isEqualTo(CategoryValidator.Verdict.REJECT);
    }

    @Test
    void treatsIndiaMentionedBesideAnotherCountryAsUncertain() {
        // Both India and the US lead the headline — a judgement call, not a guess.
        assertThat(validator.validate(
                article("United States passes rule; India among affected markets", "", "Reuters"), "india"))
                .isEqualTo(CategoryValidator.Verdict.UNCERTAIN);
    }

    // ---- Topic categories ----

    @Test
    void acceptsGenuineTechnologyStory() {
        assertThat(validator.validate(
                article("New AI chip doubles inference speed for developers", "Semiconductor research.", "Wired"),
                "technology"))
                .isEqualTo(CategoryValidator.Verdict.ACCEPT);
    }

    @Test
    void rejectsSportsStoryFiledUnderTechnology() {
        assertThat(validator.validate(
                article("Cricket World Cup final: match ends in a last-over win", "Wicket, innings, coach.", "ESPN"),
                "technology"))
                .isEqualTo(CategoryValidator.Verdict.REJECT);
    }

    @Test
    void doesNotGuessWhenThereIsNoTopicSignal() {
        assertThat(validator.validate(article("A quiet morning downtown", "", "Local"), "technology"))
                .isEqualTo(CategoryValidator.Verdict.UNCERTAIN);
    }

    @Test
    void rejectsAnArticleWithNoTitle() {
        assertThat(validator.validate(article("", "body only", "X"), "technology"))
                .isEqualTo(CategoryValidator.Verdict.REJECT);
    }
}

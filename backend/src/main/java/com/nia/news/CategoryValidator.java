package com.nia.news;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Decides whether an article genuinely belongs in the category it was fetched
 * under. Providers are unreliable here: a query for "india" routinely returns
 * global filler, so the fetched category alone is not evidence.
 *
 * The check is deliberately cheap and deterministic — it runs on every article —
 * and reports {@link Verdict#UNCERTAIN} when it cannot decide, so the caller can
 * spend a bounded number of AI classifications only on the genuinely ambiguous
 * ones.
 *
 * Two rules matter most, both learned from real misfiled articles:
 *
 *  1. The <em>main subject</em> decides, so the title is weighted far above the
 *     description. A passing mention in the body never carries a category.
 *  2. Geographic categories need geographic evidence. "India" is not satisfied by
 *     the word "India" appearing once — but it IS satisfied by an Indian state,
 *     city, institution, or an Indian publisher, which is how real Indian stories
 *     ("Karnataka High Court...", "Punjab Governor... Ludhiana") actually read.
 */
@Component
public class CategoryValidator {

    public enum Verdict {
        /** Clear evidence the article belongs here. */
        ACCEPT,
        /** Clear evidence it does not — do not store it under this category. */
        REJECT,
        /** Not enough signal either way; the caller may ask the AI service. */
        UNCERTAIN
    }

    /** Categories that carry a geographic requirement as well as a topic. */
    private static final Set<String> GEOGRAPHIC = Set.of("india");

    /**
     * India evidence. Deliberately broad: states, major cities, institutions and
     * everyday terms, because genuine Indian stories often never say "India".
     */
    private static final Set<String> INDIA_TERMS = Set.of(
            "india", "indian", "indians", "bharat", "new delhi", "delhi", "mumbai", "bengaluru",
            "bangalore", "kolkata", "chennai", "hyderabad", "pune", "ahmedabad", "jaipur", "lucknow",
            "kanpur", "nagpur", "indore", "bhopal", "patna", "surat", "ludhiana", "kochi", "noida",
            "gurugram", "gurgaon", "chandigarh", "guwahati", "varanasi", "amritsar", "udaipur",
            "maharashtra", "karnataka", "kerala", "tamil nadu", "gujarat", "rajasthan", "punjab",
            "haryana", "bihar", "odisha", "assam", "telangana", "andhra pradesh", "uttar pradesh",
            "madhya pradesh", "west bengal", "jharkhand", "chhattisgarh", "uttarakhand", "goa",
            "himachal", "manipur", "meghalaya", "nagaland", "tripura", "sikkim", "mizoram",
            "jammu", "kashmir", "ladakh", "shamli", "modi", "rahul gandhi", "bjp", "congress party",
            "aam aadmi", "lok sabha", "rajya sabha", "supreme court of india", "high court",
            "rupee", "rupees", "crore", "lakh", "rbi", "sebi", "nifty", "sensex", "upi", "gst",
            "isro", "iit", "iim", "ipl", "bcci", "aadhaar", "cbse", "neet", "upsc");

    /** Publishers that are unambiguously Indian outlets. */
    private static final List<String> INDIA_SOURCES = List.of(
            "ndtv", "times of india", "timesofindia", "indian express", "indianexpress",
            "hindustan times", "hindustantimes", "the hindu", "thehindu", "india today",
            "indiatoday", "news18", "firstpost", "livemint", "mint", "economic times",
            "economictimes", "business standard", "businessstandard", "moneycontrol", "scroll.in",
            "the wire", "thewire", "deccan", "telegraphindia", "dnaindia", "zeenews",
            "republicworld", "opindia", "swarajya", "thequint", "quint", "udaipurkiran",
            "edexlive", "newindianexpress", "tribuneindia", "freepressjournal", "abplive",
            "jagran", "amarujala", "bhaskar", "prabhasakshi", "lokmat", "eenadu", "mathrubhumi",
            "manoramaonline", "ani news", "aninews", "pti", "outlookindia", "rediff");

    /**
     * Countries that, when they dominate the headline, mean the story is not
     * primarily an Indian one.
     */
    private static final Set<String> COMPETING_PLACES = Set.of(
            "united states", "u.s.", "us ", "america", "american", "washington", "new york",
            "california", "texas", "florida", "chicago", "salt lake city", "britain", "british",
            "uk ", "london", "scotland", "wales", "ireland", "canada", "canadian", "toronto",
            "australia", "australian", "sydney", "melbourne", "china", "chinese", "beijing",
            "shanghai", "russia", "russian", "moscow", "ukraine", "france", "french", "paris",
            "germany", "german", "berlin", "japan", "japanese", "tokyo", "pakistan", "islamabad",
            "poland", "belarus", "israel", "gaza", "brazil", "mexico", "nigeria", "korea");

    /** Topic evidence per NIA category. Scored, never a single-keyword match. */
    private static final Map<String, Set<String>> TOPIC_TERMS = Map.of(
            "technology", Set.of("tech", "software", "hardware", "app", "ai", "artificial intelligence",
                    "chip", "semiconductor", "startup", "cyber", "data", "cloud", "device", "smartphone",
                    "computer", "internet", "robot", "algorithm", "developer", "gadget", "quantum"),
            "business", Set.of("market", "stock", "shares", "economy", "economic", "revenue", "profit",
                    "earnings", "investor", "investment", "trade", "company", "merger", "acquisition",
                    "ipo", "inflation", "bank", "finance", "financial", "ceo", "startup funding"),
            "science", Set.of("research", "researchers", "study", "scientist", "scientists", "nasa",
                    "space", "climate", "physics", "biology", "chemistry", "discovery", "experiment",
                    "telescope", "genome", "species", "fossil", "orbit", "particle"),
            "sports", Set.of("match", "cricket", "football", "soccer", "tennis", "olympic", "tournament",
                    "league", "player", "coach", "goal", "score", "championship", "world cup", "athlete",
                    "wicket", "innings", "striker", "nba", "nfl", "fifa"),
            "health", Set.of("health", "hospital", "doctor", "patient", "disease", "vaccine", "covid",
                    "cancer", "mental health", "medicine", "medical", "drug", "therapy", "clinical",
                    "surgery", "diagnosis", "outbreak", "who"),
            "entertainment", Set.of("film", "movie", "actor", "actress", "singer", "album", "music",
                    "celebrity", "box office", "netflix", "series", "show", "concert", "hollywood",
                    "bollywood", "trailer", "director", "award"),
            "politics", Set.of("election", "government", "minister", "parliament", "senate", "president",
                    "prime minister", "policy", "vote", "voters", "campaign", "party", "law", "bill",
                    "congress", "governor", "diplomat", "sanctions", "referendum"),
            "world", Set.of("global", "international", "united nations", "summit", "treaty", "border",
                    "war", "conflict", "refugee", "diplomatic", "foreign", "worldwide"),
            "india", Set.of());   // india is validated geographically, not by topic words

    /**
     * Terms are matched on WORD BOUNDARIES, never as substrings. Plain
     * {@code contains} silently matched "goa" inside "goal", "ipl" inside
     * "multiple" and "upi" inside "occupied", which quietly passed unrelated
     * articles as Indian news. Patterns are compiled once and reused.
     */
    private static final Pattern INDIA_PATTERN = wordPattern(INDIA_TERMS);
    private static final Pattern COMPETING_PATTERN = wordPattern(COMPETING_PLACES);
    private static final Map<String, Pattern> TOPIC_PATTERNS = compileTopics();

    private static Pattern wordPattern(Set<String> terms) {
        String alternation = terms.stream()
                .map(Pattern::quote)
                .reduce((a, b) -> a + "|" + b)
                .orElse("(?!)");
        return Pattern.compile("\\b(?:" + alternation + ")\\b", Pattern.CASE_INSENSITIVE);
    }

    private static Map<String, Pattern> compileTopics() {
        Map<String, Pattern> compiled = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : TOPIC_TERMS.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                compiled.put(entry.getKey(), wordPattern(entry.getValue()));
            }
        }
        return compiled;
    }

    /**
     * Decide whether {@code article} belongs in {@code category}.
     * Never throws; unknown categories are accepted unchanged.
     */
    public Verdict validate(NormalizedArticle article, String category) {
        if (article == null || category == null) {
            return Verdict.UNCERTAIN;
        }
        String cat = category.trim().toLowerCase(Locale.ROOT);
        String title = safeLower(article.title());
        String description = safeLower(article.description());
        String source = safeLower(article.source());

        if (title.isBlank()) {
            return Verdict.REJECT;   // nothing to judge on
        }
        if (GEOGRAPHIC.contains(cat)) {
            return validateIndia(title, description, source);
        }
        return validateTopic(cat, title, description);
    }

    /**
     * India needs India to be the subject, not a mention. Evidence in the title,
     * or an Indian publisher, is decisive; body-only evidence is weak and loses to
     * a headline that is clearly about somewhere else.
     */
    private Verdict validateIndia(String title, String description, String source) {
        // The publisher name is direct evidence, and the curated list can never be
        // complete: "Autocar India", "PM India", "Kerala Kaumudi" are all obviously
        // Indian outlets that no hand-written list had. Matching India terms in the
        // source name too catches them. This also replaces a signal that used to
        // arrive by accident - one provider concatenated the publisher into the
        // description, so the body check saw it; that description is no longer
        // stored, and without this these stories were being dropped.
        boolean indianPublisher = INDIA_SOURCES.stream().anyMatch(source::contains)
                || INDIA_PATTERN.matcher(source).find();
        if (indianPublisher) {
            return Verdict.ACCEPT;   // an Indian outlet's own coverage
        }
        boolean inTitle = INDIA_PATTERN.matcher(title).find();
        boolean inBody = INDIA_PATTERN.matcher(description).find();
        boolean competingHeadline = COMPETING_PATTERN.matcher(title).find();

        if (inTitle && !competingHeadline) {
            return Verdict.ACCEPT;
        }
        if (inTitle) {
            // Both India and another country lead the headline — genuinely ambiguous.
            return Verdict.UNCERTAIN;
        }
        if (!inBody) {
            return Verdict.REJECT;   // no India signal anywhere
        }
        // Mentioned only in the body: an India-primary story would normally say so
        // in the headline, and a competing headline settles it.
        return competingHeadline ? Verdict.REJECT : Verdict.UNCERTAIN;
    }

    /**
     * A topic category is satisfied when its own terms lead, and no other category
     * clearly dominates the headline.
     */
    private Verdict validateTopic(String category, String title, String description) {
        Pattern own_p = TOPIC_PATTERNS.get(category);
        if (own_p == null) {
            return Verdict.UNCERTAIN;   // unknown category: leave it to the caller
        }
        int own = score(title, description, own_p);

        // Does a different category own the headline more strongly?
        int rivalScore = 0;
        for (Map.Entry<String, Pattern> entry : TOPIC_PATTERNS.entrySet()) {
            if (entry.getKey().equals(category)) {
                continue;
            }
            rivalScore = Math.max(rivalScore, score(title, description, entry.getValue()));
        }

        if (own == 0) {
            // No evidence for this category. If another subject clearly leads the
            // headline that is evidence enough to reject; otherwise stay silent and
            // let the caller decide rather than guessing.
            return rivalScore >= TITLE_WEIGHT ? Verdict.REJECT : Verdict.UNCERTAIN;
        }
        if (rivalScore > own * 2) {
            return Verdict.REJECT;   // clearly a different subject
        }
        return Verdict.ACCEPT;
    }

    /** A single title hit — enough to say what a headline is about. */
    private static final int TITLE_WEIGHT = 3;

    /**
     * Distinct term hits, with the title weighted far above the body because the
     * headline carries the article's main subject.
     */
    private int score(String title, String description, Pattern pattern) {
        int total = 0;
        var inTitle = pattern.matcher(title);
        while (inTitle.find()) {
            total += TITLE_WEIGHT;
        }
        var inBody = pattern.matcher(description);
        while (inBody.find()) {
            total += 1;
        }
        return total;
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}

package com.nia.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Strongly-typed access to the {@code nia.*} settings in application.yml.
 * Secrets (provider keys, internal token) arrive from environment variables.
 */
@ConfigurationProperties(prefix = "nia")
public class NiaProperties {

    private Cors cors = new Cors();
    private String internalToken = "change-me";
    private String fastapiBaseUrl = "http://localhost:8000";
    private Rag rag = new Rag();
    private Personalization personalization = new Personalization();
    private RateLimit rateLimit = new RateLimit();
    private Ingest ingest = new Ingest();
    private News news = new News();

    public Cors getCors() { return cors; }
    public void setCors(Cors cors) { this.cors = cors; }

    public String getInternalToken() { return internalToken; }
    public void setInternalToken(String internalToken) { this.internalToken = internalToken; }

    public String getFastapiBaseUrl() { return fastapiBaseUrl; }
    public void setFastapiBaseUrl(String fastapiBaseUrl) { this.fastapiBaseUrl = fastapiBaseUrl; }

    public Rag getRag() { return rag; }
    public void setRag(Rag rag) { this.rag = rag; }

    public Personalization getPersonalization() { return personalization; }
    public void setPersonalization(Personalization personalization) { this.personalization = personalization; }

    public RateLimit getRateLimit() { return rateLimit; }
    public void setRateLimit(RateLimit rateLimit) { this.rateLimit = rateLimit; }

    public Ingest getIngest() { return ingest; }
    public void setIngest(Ingest ingest) { this.ingest = ingest; }

    public News getNews() { return news; }
    public void setNews(News news) { this.news = news; }

    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:5173");
        public List<String> getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins; }
    }

    public static class Rag {
        private int topK = 8;
        public int getTopK() { return topK; }
        public void setTopK(int topK) { this.topK = topK; }
    }

    public static class Personalization {
        /** Order: recency, favorite-category, reading-interest, bookmark-source. */
        private List<Double> weights = List.of(0.5, 0.25, 0.15, 0.1);
        public List<Double> getWeights() { return weights; }
        public void setWeights(List<Double> weights) { this.weights = weights; }
    }

    public static class RateLimit {
        private int refreshPerHour = 10;
        private int assistantPerHour = 60;
        public int getRefreshPerHour() { return refreshPerHour; }
        public void setRefreshPerHour(int refreshPerHour) { this.refreshPerHour = refreshPerHour; }
        public int getAssistantPerHour() { return assistantPerHour; }
        public void setAssistantPerHour(int assistantPerHour) { this.assistantPerHour = assistantPerHour; }
    }

    public static class Ingest {
        private boolean enabled = true;
        private String cron = "0 */10 * * * *";
        private List<String> categories = List.of(
                "technology", "business", "world", "india", "science",
                "sports", "health", "entertainment", "politics");
        private List<String> countries = List.of("us", "in", "gb");
        private String defaultLanguage = "en";
        private int pageSize = 10;
        /** Max articles embedded per ingestion cycle (keeps embedding within the provider rate limit). */
        private int embedMaxPerCycle = 100;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getCron() { return cron; }
        public void setCron(String cron) { this.cron = cron; }
        public List<String> getCategories() { return categories; }
        public void setCategories(List<String> categories) { this.categories = categories; }
        public List<String> getCountries() { return countries; }
        public void setCountries(List<String> countries) { this.countries = countries; }
        public String getDefaultLanguage() { return defaultLanguage; }
        public void setDefaultLanguage(String defaultLanguage) { this.defaultLanguage = defaultLanguage; }
        public int getPageSize() { return pageSize; }
        public void setPageSize(int pageSize) { this.pageSize = pageSize; }
        public int getEmbedMaxPerCycle() { return embedMaxPerCycle; }
        public void setEmbedMaxPerCycle(int embedMaxPerCycle) { this.embedMaxPerCycle = embedMaxPerCycle; }
    }

    public static class News {
        private String primary = "GNEWS";
        private String secondary = "NEWSDATA";
        private List<String> fallback = List.of("GUARDIAN", "CURRENTS", "GOOGLE_NEWS_RSS");
        private String gnewsKey = "";
        private String newsdataKey = "";
        private String guardianKey = "";
        private String currentsKey = "";
        private String newsapiKey = "";

        public String getPrimary() { return primary; }
        public void setPrimary(String primary) { this.primary = primary; }
        public String getSecondary() { return secondary; }
        public void setSecondary(String secondary) { this.secondary = secondary; }
        public List<String> getFallback() { return fallback; }
        public void setFallback(List<String> fallback) { this.fallback = fallback; }
        public String getGnewsKey() { return gnewsKey; }
        public void setGnewsKey(String gnewsKey) { this.gnewsKey = gnewsKey; }
        public String getNewsdataKey() { return newsdataKey; }
        public void setNewsdataKey(String newsdataKey) { this.newsdataKey = newsdataKey; }
        public String getGuardianKey() { return guardianKey; }
        public void setGuardianKey(String guardianKey) { this.guardianKey = guardianKey; }
        public String getCurrentsKey() { return currentsKey; }
        public void setCurrentsKey(String currentsKey) { this.currentsKey = currentsKey; }
        public String getNewsapiKey() { return newsapiKey; }
        public void setNewsapiKey(String newsapiKey) { this.newsapiKey = newsapiKey; }

        /** Providers in priority order: primary, secondary, then fallbacks. */
        public List<String> priorityOrder() {
            var order = new java.util.ArrayList<String>();
            if (primary != null && !primary.isBlank()) order.add(primary.trim().toUpperCase());
            if (secondary != null && !secondary.isBlank()) order.add(secondary.trim().toUpperCase());
            if (fallback != null) {
                for (String f : fallback) {
                    if (f != null && !f.isBlank()) order.add(f.trim().toUpperCase());
                }
            }
            return order;
        }
    }
}

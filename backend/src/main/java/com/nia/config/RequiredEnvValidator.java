package com.nia.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Fails fast with a clear message when a required backend secret is missing, so
 * misconfiguration surfaces at startup instead of as broken behavior later.
 * Optional providers (news/LLM keys) only warn — they degrade gracefully.
 * Secret values are never logged.
 */
@Component
public class RequiredEnvValidator {

    private static final Logger log = LoggerFactory.getLogger(RequiredEnvValidator.class);

    private final SupabaseProperties supabase;
    private final NiaProperties nia;

    public RequiredEnvValidator(SupabaseProperties supabase, NiaProperties nia) {
        this.supabase = supabase;
        this.nia = nia;
    }

    @PostConstruct
    public void validate() {
        List<String> missing = new ArrayList<>();
        if (isBlank(supabase.getJwtSecret())) missing.add("SUPABASE_JWT_SECRET");
        if (isBlank(nia.getInternalToken())) missing.add("NIA_INTERNAL_TOKEN");

        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "Missing required environment variable(s): " + String.join(", ", missing)
                            + ". Set them in the root .env (see .env.example). Values are never logged.");
        }

        boolean anyNewsKey = !isBlank(nia.getNews().getGnewsKey())
                || !isBlank(nia.getNews().getNewsdataKey())
                || !isBlank(nia.getNews().getGuardianKey())
                || !isBlank(nia.getNews().getCurrentsKey());
        if (!anyNewsKey) {
            log.warn("No news provider API keys configured — the feed will be empty until at least one is set "
                    + "(e.g. GNEWS_API_KEY). Google News RSS needs no key and remains available.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

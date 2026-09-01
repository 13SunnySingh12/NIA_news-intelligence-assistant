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

    /** The placeholder in application.yml. Usable locally, never in a deployment. */
    static final String INSECURE_TOKEN_DEFAULT = "change-me";

    @PostConstruct
    public void validate() {
        List<String> missing = new ArrayList<>();
        if (isBlank(supabase.getJwtSecret())) missing.add("SUPABASE_JWT_SECRET");
        if (isBlank(nia.getInternalToken())) missing.add("NIA_INTERNAL_TOKEN");
        // Supabase signs access tokens with ES256 and the verification keys are
        // fetched from <SUPABASE_URL>/auth/v1/.well-known/jwks.json. Without this
        // the app still starts and reports healthy, but every signed-in request
        // fails with 401 because only the legacy HS256 path remains. That is a
        // silent, confusing production failure, so refuse to start instead.
        if (isBlank(supabase.getUrl())) missing.add("SUPABASE_URL");

        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "Missing required environment variable(s): " + String.join(", ", missing)
                            + ". Set them in the root .env (see docs/environment.md). Values are never logged.");
        }

        // An unset NIA_INTERNAL_TOKEN falls back to the placeholder rather than to
        // blank, so the check above cannot see it. Both services would then trust a
        // value published in this repository, leaving the AI service open to anyone
        // who reads it.
        if (INSECURE_TOKEN_DEFAULT.equals(nia.getInternalToken())) {
            throw new IllegalStateException(
                    "NIA_INTERNAL_TOKEN is still set to the insecure placeholder. Generate a random value "
                            + "(e.g. `openssl rand -hex 32`) and set the SAME value on the backend and the AI "
                            + "service. Values are never logged.");
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

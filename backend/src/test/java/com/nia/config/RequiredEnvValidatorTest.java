package com.nia.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * These guard against misconfigurations that a deployment would otherwise
 * survive: the app starts, reports healthy, and only then behaves wrongly.
 * Each case here was reachable in a real deploy.
 */
class RequiredEnvValidatorTest {

    private static final String URL = "https://project.supabase.co";
    private static final String JWT = "a-supabase-jwt-secret-long-enough-for-hs256";
    private static final String TOKEN = "9f2c1b7ae4d3086552cf1a9e7b4d2c60";

    private SupabaseProperties supabase(String url, String jwtSecret) {
        SupabaseProperties p = new SupabaseProperties();
        p.setUrl(url);
        p.setJwtSecret(jwtSecret);
        return p;
    }

    private NiaProperties nia(String internalToken) {
        NiaProperties p = new NiaProperties();
        p.setInternalToken(internalToken);
        return p;
    }

    @Test
    void acceptsAFullyConfiguredEnvironment() {
        assertThatCode(() -> new RequiredEnvValidator(supabase(URL, JWT), nia(TOKEN)).validate())
                .doesNotThrowAnyException();
    }

    /**
     * The failure this exists to prevent: Supabase signs access tokens with ES256
     * and the public keys come from SUPABASE_URL's JWKS endpoint. Without it the
     * service starts, /api/health returns 200, and every authenticated request
     * returns 401 — which reads like a broken login, not a missing variable.
     */
    @Test
    void refusesToStartWithoutSupabaseUrl() {
        assertThatThrownBy(() -> new RequiredEnvValidator(supabase(null, JWT), nia(TOKEN)).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SUPABASE_URL");
    }

    @Test
    void refusesToStartWithoutJwtSecret() {
        assertThatThrownBy(() -> new RequiredEnvValidator(supabase(URL, "  "), nia(TOKEN)).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SUPABASE_JWT_SECRET");
    }

    @Test
    void refusesToStartWithABlankInternalToken() {
        assertThatThrownBy(() -> new RequiredEnvValidator(supabase(URL, JWT), nia("")).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("NIA_INTERNAL_TOKEN");
    }

    /**
     * An unset NIA_INTERNAL_TOKEN resolves to the placeholder in application.yml,
     * not to blank, so a blank check alone cannot catch it. Both services would
     * then share a secret published in this repository.
     */
    @Test
    void refusesToStartWithThePlaceholderInternalToken() {
        assertThatThrownBy(() ->
                new RequiredEnvValidator(supabase(URL, JWT),
                        nia(RequiredEnvValidator.INSECURE_TOKEN_DEFAULT)).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("insecure placeholder");
    }

    @Test
    void reportsEveryMissingVariableAtOnce() {
        assertThatThrownBy(() -> new RequiredEnvValidator(supabase(null, null), nia(TOKEN)).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SUPABASE_JWT_SECRET")
                .hasMessageContaining("SUPABASE_URL");
    }

    @Test
    void neverPutsASecretValueInTheFailureMessage() {
        String message = "";
        try {
            new RequiredEnvValidator(supabase(URL, JWT),
                    nia(RequiredEnvValidator.INSECURE_TOKEN_DEFAULT)).validate();
        } catch (IllegalStateException ex) {
            message = ex.getMessage();
        }
        // The message may name variables, never their contents.
        assertThat(message).doesNotContain(JWT).doesNotContain(TOKEN);
    }
}

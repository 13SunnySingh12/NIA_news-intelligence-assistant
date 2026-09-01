package com.nia.auth;

import com.nia.config.SupabaseProperties;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Locator;
import io.jsonwebtoken.security.Jwk;
import io.jsonwebtoken.security.Jwks;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Finds the right key to verify a Supabase JWT with.
 *
 * Supabase signs access tokens in one of two ways, and a project can use either:
 *   • asymmetric (ES256/RS256) — the current default. Tokens carry a "kid" and are
 *     verified with the matching public key from the project's JWKS endpoint.
 *   • symmetric (HS256) — the legacy shared secret in SUPABASE_JWT_SECRET.
 *
 * Supporting both means the backend keeps working across a key rotation instead of
 * rejecting every real user's token. Keys are fetched lazily and cached; an unknown
 * "kid" triggers at most one refresh per cooldown so a bad token can't hammer Supabase.
 */
@Component
public class SupabaseJwtKeyLocator implements Locator<Key> {

    private static final Logger log = LoggerFactory.getLogger(SupabaseJwtKeyLocator.class);
    private static final Duration REFRESH_COOLDOWN = Duration.ofMinutes(5);

    private final String jwksUrl;
    private final SecretKey hmacKey;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private volatile Map<String, Key> keysByKid = Map.of();
    private volatile long lastRefreshMs = 0L;

    public SupabaseJwtKeyLocator(SupabaseProperties props) {
        String url = props.getUrl() == null ? "" : props.getUrl().trim();
        this.jwksUrl = url.isBlank() ? null : url.replaceAll("/+$", "") + "/auth/v1/.well-known/jwks.json";

        String secret = props.getJwtSecret();
        this.hmacKey = (secret != null && secret.getBytes(StandardCharsets.UTF_8).length >= 32)
                ? Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))
                : null;

        if (jwksUrl == null && hmacKey == null) {
            log.warn("No Supabase JWT verification key configured (need SUPABASE_URL or SUPABASE_JWT_SECRET); "
                    + "all requests will be treated as unauthenticated.");
        }
    }

    /** True when at least one verification method is available. */
    public boolean isConfigured() {
        return jwksUrl != null || hmacKey != null;
    }

    @Override
    public Key locate(Header header) {
        if (!(header instanceof JwsHeader jws)) {
            return null;
        }
        String kid = jws.getKeyId();
        // Symmetric tokens (legacy projects) carry no kid — verify with the shared secret.
        if (kid == null || kid.isBlank()) {
            return hmacKey;
        }
        Key key = keysByKid.get(kid);
        if (key == null) {
            refreshKeys();                 // key may be new (rotation) — try once
            key = keysByKid.get(kid);
        }
        // Unknown kid: fall back to the shared secret rather than failing outright.
        return key != null ? key : hmacKey;
    }

    /** Re-fetch the JWKS, at most once per cooldown. Failures leave the cache untouched. */
    private synchronized void refreshKeys() {
        if (jwksUrl == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastRefreshMs < REFRESH_COOLDOWN.toMillis() && !keysByKid.isEmpty()) {
            return;
        }
        lastRefreshMs = now;
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(jwksUrl))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("Could not load Supabase JWKS (HTTP {})", response.statusCode());
                return;
            }
            Map<String, Key> parsed = new HashMap<>();
            for (Jwk<?> jwk : Jwks.setParser().build().parse(response.body())) {
                if (jwk.getId() != null) {
                    parsed.put(jwk.getId(), jwk.toKey());
                }
            }
            if (!parsed.isEmpty()) {
                keysByKid = Map.copyOf(parsed);
                log.info("Loaded {} Supabase JWKS signing key(s).", parsed.size());
            }
        } catch (Exception ex) {
            // Never fail a request because the key refresh failed; the cached keys stand.
            log.warn("Supabase JWKS refresh failed: {}", ex.getClass().getSimpleName());
        }
    }
}

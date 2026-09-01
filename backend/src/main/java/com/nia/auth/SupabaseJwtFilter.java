package com.nia.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Verifies the Supabase-issued JWT sent as {@code Authorization: Bearer <token>}
 * and populates the security context with the user id ({@code sub}).
 *
 * The signing key is resolved per token by {@link SupabaseJwtKeyLocator}, which
 * supports both the asymmetric (ES256/RS256, via the project's JWKS) and the
 * legacy symmetric (HS256 shared secret) schemes Supabase can be configured with.
 * Requests without a valid token are left unauthenticated so the security config
 * can reject protected routes with 401.
 */
@Component
public class SupabaseJwtFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SupabaseJwtFilter.class);

    private final SupabaseJwtKeyLocator keyLocator;

    public SupabaseJwtFilter(SupabaseJwtKeyLocator keyLocator) {
        // Fail closed: with no verification key configured, authenticate no one.
        this.keyLocator = keyLocator;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (keyLocator.isConfigured() && header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            try {
                Claims claims = Jwts.parser()
                        .keyLocator(keyLocator)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                String userId = claims.getSubject();
                if (userId != null && !userId.isBlank()) {
                    var authentication = new UsernamePasswordAuthenticationToken(
                            userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception ex) {
                // Invalid/expired token: stay unauthenticated. Never leak details.
                log.debug("JWT verification failed: {}", ex.getClass().getSimpleName());
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }
}

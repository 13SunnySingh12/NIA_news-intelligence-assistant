package com.nia.auth;

import com.nia.config.SupabaseProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SupabaseJwtFilterTest {

    private static final String SECRET = "a-test-secret-that-is-definitely-long-enough-32+bytes";

    private SupabaseJwtFilter filter() {
        SupabaseProperties props = new SupabaseProperties();
        props.setJwtSecret(SECRET);
        // No SUPABASE_URL here, so the locator uses the legacy HS256 secret path.
        return new SupabaseJwtFilter(new SupabaseJwtKeyLocator(props));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesValidToken() throws Exception {
        String userId = "11111111-1111-1111-1111-111111111111";
        String token = Jwts.builder()
                .subject(userId)
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        FilterChain chain = mock(FilterChain.class);

        filter().doFilter(request, new MockHttpServletResponse(), chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(userId);
    }

    @Test
    void rejectsTamperedToken() throws Exception {
        String forged = Jwts.builder()
                .subject("intruder")
                .signWith(Keys.hmacShaKeyFor("a-completely-different-secret-key-32-bytes!!".getBytes(StandardCharsets.UTF_8)))
                .compact();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + forged);

        filter().doFilter(request, new MockHttpServletResponse(), mock(FilterChain.class));

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void ignoresMissingHeader() throws Exception {
        filter().doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), mock(FilterChain.class));
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}

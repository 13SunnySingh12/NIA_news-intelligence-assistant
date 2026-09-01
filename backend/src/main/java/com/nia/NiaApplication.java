package com.nia;

import com.nia.config.NiaProperties;
import com.nia.config.SupabaseProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the NIA Spring Boot backend — the single REST API the
 * frontend talks to. It aggregates news, serves articles/bookmarks/history/
 * trending/preferences, and proxies AI requests to the FastAPI service.
 *
 * Authentication is stateless Supabase-JWT verification (see SupabaseJwtFilter),
 * so Spring Security's default form-login user is disabled — it would otherwise
 * create an unused in-memory user and log a "generated security password".
 */
@SpringBootApplication(exclude = { UserDetailsServiceAutoConfiguration.class })
@EnableScheduling
@EnableConfigurationProperties({NiaProperties.class, SupabaseProperties.class})
public class NiaApplication {

    public static void main(String[] args) {
        SpringApplication.run(NiaApplication.class, args);
    }
}

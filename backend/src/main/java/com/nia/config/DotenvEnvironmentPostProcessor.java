package com.nia.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads the single root {@code .env} into the Spring environment for LOCAL
 * development, so all three services can share one file. Registered as the
 * lowest-precedence source, so real environment variables (Render/Docker) always
 * win — production never depends on a committed file. Secret values are never logged.
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final int MAX_PARENTS_TO_SEARCH = 4;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        Path dotenv = findDotenv();
        if (dotenv == null) {
            return;
        }
        Map<String, Object> values = parse(dotenv);
        if (!values.isEmpty()) {
            // addLast => lowest precedence: OS env vars and -D system properties override it.
            environment.getPropertySources().addLast(new MapPropertySource("niaDotenv", values));
        }
    }

    /** Walk up from the working directory to find the nearest .env (repo root). */
    private Path findDotenv() {
        Path dir = Paths.get("").toAbsolutePath();
        for (int i = 0; i <= MAX_PARENTS_TO_SEARCH && dir != null; i++) {
            Path candidate = dir.resolve(".env");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            dir = dir.getParent();
        }
        return null;
    }

    private Map<String, Object> parse(Path file) {
        Map<String, Object> values = new LinkedHashMap<>();
        try {
            for (String raw : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String line = stripBom(raw).trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith("export ")) {
                    line = line.substring("export ".length()).trim();
                }
                int eq = line.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = line.substring(0, eq).trim();
                String value = stripQuotes(line.substring(eq + 1).trim());
                if (!key.isEmpty()) {
                    values.put(key, value);
                }
            }
        } catch (IOException ignored) {
            // A missing/unreadable .env is fine — real env vars may supply everything.
        }
        return values;
    }

    private String stripBom(String s) {
        return (!s.isEmpty() && s.charAt(0) == '﻿') ? s.substring(1) : s;
    }

    private String stripQuotes(String value) {
        if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}

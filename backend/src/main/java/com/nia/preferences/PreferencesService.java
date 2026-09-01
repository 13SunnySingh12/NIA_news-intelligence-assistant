package com.nia.preferences;

import com.nia.common.ApiException;
import com.nia.news.CategoryMapper;
import com.nia.preferences.model.UserPreferences;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Reads and updates a user's feed preferences, persisting through Supabase. */
@Service
public class PreferencesService {

    private final PreferencesRepository repository;
    private final CategoryMapper categoryMapper;

    public PreferencesService(PreferencesRepository repository, CategoryMapper categoryMapper) {
        this.repository = repository;
        this.categoryMapper = categoryMapper;
    }

    public PreferencesDto get(String userId) {
        return repository.findById(UUID.fromString(userId))
                .map(PreferencesDto::of)
                .orElseGet(PreferencesDto::defaults);
    }

    /** Ensures a preferences row exists for the user (used by the optional bootstrap endpoint). */
    @Transactional
    public PreferencesDto ensureExists(String userId) {
        UUID uid = UUID.fromString(userId);
        UserPreferences prefs = repository.findById(uid).orElseGet(() -> {
            UserPreferences created = new UserPreferences(uid);
            return repository.save(created);
        });
        return PreferencesDto.of(prefs);
    }

    @Transactional
    public PreferencesDto update(String userId, PreferencesUpdateRequest request) {
        UUID uid = UUID.fromString(userId);
        UserPreferences prefs = repository.findById(uid).orElseGet(() -> new UserPreferences(uid));

        if (request.favoriteCategories() != null) {
            validateCategories(request.favoriteCategories());
            prefs.setFavoriteCategories(request.favoriteCategories().stream()
                    .map(String::toLowerCase).distinct().toArray(String[]::new));
        }
        if (request.languages() != null && !request.languages().isEmpty()) {
            prefs.setLanguages(request.languages().stream()
                    .map(String::toLowerCase).distinct().toArray(String[]::new));
        }
        if (request.countries() != null && !request.countries().isEmpty()) {
            prefs.setCountries(request.countries().stream()
                    .map(String::toLowerCase).distinct().toArray(String[]::new));
        }
        prefs.setUpdatedAt(Instant.now());
        return PreferencesDto.of(repository.save(prefs));
    }

    private void validateCategories(List<String> categories) {
        for (String category : categories) {
            if (!categoryMapper.isValid(category)) {
                throw ApiException.badRequest("Unknown category: " + category);
            }
        }
    }
}

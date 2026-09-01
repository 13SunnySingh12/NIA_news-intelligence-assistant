package com.nia.preferences;

import com.nia.preferences.model.UserPreferences;

import java.util.List;

/** The preferences shape exchanged with the frontend. */
public record PreferencesDto(
        List<String> favoriteCategories,
        List<String> languages,
        List<String> countries
) {
    public static PreferencesDto of(UserPreferences prefs) {
        return new PreferencesDto(
                List.of(prefs.getFavoriteCategories()),
                List.of(prefs.getLanguages()),
                List.of(prefs.getCountries()));
    }

    public static PreferencesDto defaults() {
        return new PreferencesDto(List.of(), List.of("en"), List.of("us"));
    }
}

package com.nia.preferences;

import java.util.List;

/** Body for PUT /api/preferences. Any null list is treated as "leave unchanged". */
public record PreferencesUpdateRequest(
        List<String> favoriteCategories,
        List<String> languages,
        List<String> countries
) {
}

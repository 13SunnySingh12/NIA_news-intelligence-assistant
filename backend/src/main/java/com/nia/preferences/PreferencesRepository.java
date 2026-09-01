package com.nia.preferences;

import com.nia.preferences.model.UserPreferences;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PreferencesRepository extends JpaRepository<UserPreferences, UUID> {
}

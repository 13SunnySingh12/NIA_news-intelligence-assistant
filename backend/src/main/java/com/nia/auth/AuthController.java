package com.nia.auth;

import com.nia.preferences.PreferencesDto;
import com.nia.preferences.PreferencesService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Optional bootstrap: called by the frontend right after sign-in to make sure the
 * user has a preferences row. Safe to call repeatedly.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final PreferencesService preferencesService;
    private final UserContext userContext;

    public AuthController(PreferencesService preferencesService, UserContext userContext) {
        this.preferencesService = preferencesService;
        this.userContext = userContext;
    }

    @PostMapping("/sync")
    public PreferencesDto sync() {
        return preferencesService.ensureExists(userContext.requireUserId());
    }
}

package com.nia.preferences;

import com.nia.auth.UserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/preferences")
public class PreferencesController {

    private final PreferencesService preferencesService;
    private final UserContext userContext;

    public PreferencesController(PreferencesService preferencesService, UserContext userContext) {
        this.preferencesService = preferencesService;
        this.userContext = userContext;
    }

    @GetMapping
    public PreferencesDto get() {
        return preferencesService.get(userContext.requireUserId());
    }

    @PutMapping
    public PreferencesDto update(@RequestBody PreferencesUpdateRequest request) {
        return preferencesService.update(userContext.requireUserId(), request);
    }
}

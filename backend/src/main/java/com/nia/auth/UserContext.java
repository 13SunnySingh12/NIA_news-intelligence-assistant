package com.nia.auth;

import com.nia.common.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Convenience accessor for the authenticated Supabase user id (the JWT {@code sub}). */
@Component
public class UserContext {

    /** The current user's id, or throws 401 if the request is not authenticated. */
    public String requireUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) {
            throw ApiException.unauthorized();
        }
        Object principal = auth.getPrincipal();
        if (!(principal instanceof String userId) || userId.isBlank() || "anonymousUser".equals(userId)) {
            throw ApiException.unauthorized();
        }
        return userId;
    }
}

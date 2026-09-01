package com.nia.operations;

import com.nia.auth.UserContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Read-only status API for backend operations. The frontend polls these purely
 * for UI visibility — the actual work runs on the backend regardless.
 */
@RestController
@RequestMapping("/api/operations")
public class OperationController {

    private final OperationService operationService;
    private final UserContext userContext;

    public OperationController(OperationService operationService, UserContext userContext) {
        this.operationService = operationService;
        this.userContext = userContext;
    }

    /** Current state of one operation (scoped to the authenticated owner). */
    @GetMapping("/{id}")
    public OperationDto get(@PathVariable UUID id) {
        UUID userId = UUID.fromString(userContext.requireUserId());
        return operationService.getForUser(id, userId);
    }

    /**
     * The user's current in-flight operation of a type, if any. Used on page load
     * to reconnect to a running task instead of starting a new one. Returns 204
     * when there is nothing in progress.
     */
    @GetMapping("/active")
    public ResponseEntity<OperationDto> active(@RequestParam String type) {
        UUID userId = UUID.fromString(userContext.requireUserId());
        return operationService.findActiveForUser(userId, type)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}

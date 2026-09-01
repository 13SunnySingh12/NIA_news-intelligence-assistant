package com.nia.operations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nia.common.ApiException;
import com.nia.operations.model.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Creates and updates operation records — the single source of truth for
 * long-running work. The frontend only ever reads these; the backend owns every
 * state transition.
 */
@Service
public class OperationService {

    private static final Logger log = LoggerFactory.getLogger(OperationService.class);

    private final OperationRepository repository;
    private final ObjectMapper objectMapper;

    public OperationService(OperationRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Operation create(UUID userId, String type) {
        Operation operation = new Operation();
        operation.setId(UUID.randomUUID());
        operation.setUserId(userId);
        operation.setType(type);
        operation.setStatus(OperationStatus.PENDING);
        operation.setProgress(0);
        operation.setUpdatedAt(Instant.now());
        return repository.save(operation);
    }

    @Transactional
    public void markRunning(UUID id, String step, int progress) {
        Operation operation = require(id);
        if (operation.getStatus().isTerminal()) return;
        operation.setStatus(OperationStatus.RUNNING);
        if (operation.getStartedAt() == null) {
            operation.setStartedAt(Instant.now());
        }
        operation.setCurrentStep(step);
        operation.setProgress(clamp(progress));
        operation.setUpdatedAt(Instant.now());
        repository.save(operation);
    }

    @Transactional
    public void updateProgress(UUID id, int progress, String step) {
        Operation operation = require(id);
        if (operation.getStatus().isTerminal()) return; // never move a finished op backwards
        operation.setStatus(OperationStatus.RUNNING);
        operation.setProgress(clamp(progress));
        operation.setCurrentStep(step);
        operation.setUpdatedAt(Instant.now());
        repository.save(operation);
    }

    @Transactional
    public void markCompleted(UUID id, Map<String, Object> result) {
        Operation operation = require(id);
        operation.setStatus(OperationStatus.COMPLETED);
        operation.setProgress(100);
        operation.setCurrentStep("Completed");
        operation.setResult(toJson(result));
        operation.setCompletedAt(Instant.now());
        operation.setUpdatedAt(Instant.now());
        repository.save(operation);
    }

    @Transactional
    public void markFailed(UUID id, String safeMessage) {
        Operation operation = require(id);
        operation.setStatus(OperationStatus.FAILED);
        operation.setError(safeMessage);
        operation.setCompletedAt(Instant.now());
        operation.setUpdatedAt(Instant.now());
        repository.save(operation);
    }

    /** Read one operation, scoped to its owner (returns a safe DTO). */
    public OperationDto getForUser(UUID id, UUID userId) {
        Operation operation = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("That operation couldn't be found."));
        return toDto(operation);
    }

    /** The user's current PENDING/RUNNING operation of a type, if any. */
    public Optional<OperationDto> findActiveForUser(UUID userId, String type) {
        return repository.findFirstByUserIdAndTypeAndStatusInOrderByCreatedAtDesc(
                        userId, type, List.of(OperationStatus.PENDING, OperationStatus.RUNNING))
                .map(this::toDto);
    }

    /** On startup, fail any operations left open by a previous run (no task is actually running). */
    @Transactional
    public void failOpenOperationsOnStartup() {
        int failed = repository.failOpenOperations(
                OperationStatus.FAILED,
                "Interrupted by a server restart. Please try again.",
                Instant.now(),
                List.of(OperationStatus.PENDING, OperationStatus.RUNNING));
        if (failed > 0) {
            log.info("Marked {} interrupted operation(s) as FAILED on startup", failed);
        }
    }

    private Operation require(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Operation not found: " + id));
    }

    private OperationDto toDto(Operation o) {
        return new OperationDto(
                o.getId(), o.getType(), o.getStatus(), o.getProgress(), o.getCurrentStep(),
                parseResult(o.getResult()), o.getError(),
                o.getCreatedAt(), o.getStartedAt(), o.getCompletedAt());
    }

    private String toJson(Map<String, Object> result) {
        if (result == null) return null;
        try {
            return objectMapper.writeValueAsString(result);
        } catch (Exception ex) {
            return null;
        }
    }

    private Object parseResult(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception ex) {
            return json;
        }
    }

    private int clamp(int progress) {
        return Math.max(0, Math.min(100, progress));
    }
}

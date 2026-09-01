package com.nia.operations;

import java.time.Instant;
import java.util.UUID;

/**
 * The operation shape the frontend receives. Safe by construction: it never
 * carries secrets, tokens, credentials, or stack traces — only status data.
 */
public record OperationDto(
        UUID id,
        String type,
        OperationStatus status,
        int progress,
        String currentStep,
        Object result,
        String error,
        Instant createdAt,
        Instant startedAt,
        Instant completedAt
) {
}

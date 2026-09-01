package com.nia.operations;

import com.nia.operations.model.Operation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface OperationRepository extends JpaRepository<Operation, UUID> {

    /** Scoped by user so one user can never read another user's operation. */
    Optional<Operation> findByIdAndUserId(UUID id, UUID userId);

    /** The user's most recent operation of a type in any of the given states. */
    Optional<Operation> findFirstByUserIdAndTypeAndStatusInOrderByCreatedAtDesc(
            UUID userId, String type, Collection<OperationStatus> statuses);

    /**
     * Mark any still-open operations as failed. Called once on startup: after a
     * backend restart, no in-memory task is actually running, so PENDING/RUNNING
     * rows are stale and must not appear "stuck" to the frontend.
     */
    @Modifying
    @Transactional
    @Query("""
            UPDATE Operation o
            SET o.status = :failed, o.error = :message, o.completedAt = :now, o.updatedAt = :now
            WHERE o.status IN :openStatuses
            """)
    int failOpenOperations(@Param("failed") OperationStatus failed,
                           @Param("message") String message,
                           @Param("now") Instant now,
                           @Param("openStatuses") Collection<OperationStatus> openStatuses);
}

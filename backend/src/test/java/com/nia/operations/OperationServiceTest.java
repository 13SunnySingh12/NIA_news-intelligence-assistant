package com.nia.operations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nia.common.ApiException;
import com.nia.operations.model.Operation;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OperationServiceTest {

    private final OperationRepository repository = mock(OperationRepository.class);
    private final OperationService service = new OperationService(repository, new ObjectMapper());

    @Test
    void createStartsPending() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        UUID userId = UUID.randomUUID();

        Operation operation = service.create(userId, "NEWS_REFRESH");

        assertThat(operation.getStatus()).isEqualTo(OperationStatus.PENDING);
        assertThat(operation.getUserId()).isEqualTo(userId);
        assertThat(operation.getType()).isEqualTo("NEWS_REFRESH");
        assertThat(operation.getId()).isNotNull();
    }

    @Test
    void markCompletedStoresResultAndFullProgress() {
        Operation operation = new Operation();
        operation.setId(UUID.randomUUID());
        operation.setStatus(OperationStatus.RUNNING);
        when(repository.findById(operation.getId())).thenReturn(Optional.of(operation));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.markCompleted(operation.getId(), Map.of("newArticles", 7));

        assertThat(operation.getStatus()).isEqualTo(OperationStatus.COMPLETED);
        assertThat(operation.getProgress()).isEqualTo(100);
        assertThat(operation.getResult()).contains("newArticles").contains("7");
        assertThat(operation.getCompletedAt()).isNotNull();
    }

    @Test
    void terminalOperationIsNotMovedBackwards() {
        Operation operation = new Operation();
        operation.setId(UUID.randomUUID());
        operation.setStatus(OperationStatus.COMPLETED);
        operation.setProgress(100);
        when(repository.findById(operation.getId())).thenReturn(Optional.of(operation));

        service.updateProgress(operation.getId(), 20, "Updating");

        assertThat(operation.getStatus()).isEqualTo(OperationStatus.COMPLETED);
        assertThat(operation.getProgress()).isEqualTo(100);
    }

    @Test
    void getForUserMissingThrowsNotFound() {
        when(repository.findByIdAndUserId(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getForUser(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void findActiveForUserMapsToDto() {
        Operation operation = new Operation();
        operation.setId(UUID.randomUUID());
        operation.setType("NEWS_REFRESH");
        operation.setStatus(OperationStatus.RUNNING);
        when(repository.findFirstByUserIdAndTypeAndStatusInOrderByCreatedAtDesc(any(), eq("NEWS_REFRESH"), any()))
                .thenReturn(Optional.of(operation));

        Optional<OperationDto> dto = service.findActiveForUser(UUID.randomUUID(), "NEWS_REFRESH");

        assertThat(dto).isPresent();
        assertThat(dto.get().status()).isEqualTo(OperationStatus.RUNNING);
    }
}

package com.nia.news;

import com.nia.auth.UserContext;
import com.nia.common.ApiException;
import com.nia.config.NiaProperties;
import com.nia.common.RateLimiter;
import com.nia.operations.OperationDto;
import com.nia.operations.OperationService;
import com.nia.operations.OperationStatus;
import com.nia.operations.model.Operation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NewsControllerTest {

    private final OperationService operationService = mock(OperationService.class);
    private final NewsOperationRunner runner = mock(NewsOperationRunner.class);
    private final CategoryMapper categoryMapper = new CategoryMapper();
    private final UserContext userContext = mock(UserContext.class);
    private final RateLimiter rateLimiter = mock(RateLimiter.class);

    private NewsController controller;
    private final String userId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        controller = new NewsController(operationService, runner, categoryMapper,
                userContext, rateLimiter, new NiaProperties());
        when(userContext.requireUserId()).thenReturn(userId);
    }

    @Test
    void reconnectsToActiveOperationWithoutStartingAnother() {
        OperationDto active = new OperationDto(UUID.randomUUID(), NewsController.OP_NEWS_REFRESH,
                OperationStatus.RUNNING, 40, "Updating", null, null, Instant.now(), Instant.now(), null);
        when(operationService.findActiveForUser(any(), eq(NewsController.OP_NEWS_REFRESH)))
                .thenReturn(Optional.of(active));

        var response = controller.refresh(null);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(OperationStatus.RUNNING);
        verify(runner, never()).run(any(), any());
        verify(operationService, never()).create(any(), any());
        verify(rateLimiter, never()).tryAcquire(anyString(), anyString(), anyInt());
    }

    @Test
    void startsBackgroundOperationWhenNoneActive() {
        when(operationService.findActiveForUser(any(), eq(NewsController.OP_NEWS_REFRESH)))
                .thenReturn(Optional.empty());
        when(rateLimiter.tryAcquire(anyString(), anyString(), anyInt())).thenReturn(true);

        Operation created = new Operation();
        created.setId(UUID.randomUUID());
        created.setUserId(UUID.fromString(userId));
        created.setType(NewsController.OP_NEWS_REFRESH);
        created.setStatus(OperationStatus.PENDING);
        when(operationService.create(any(), eq(NewsController.OP_NEWS_REFRESH))).thenReturn(created);

        OperationDto dto = new OperationDto(created.getId(), NewsController.OP_NEWS_REFRESH,
                OperationStatus.PENDING, 0, null, null, null, Instant.now(), null, null);
        when(operationService.getForUser(created.getId(), UUID.fromString(userId))).thenReturn(dto);

        var response = controller.refresh(null);

        verify(runner).run(eq(created.getId()), isNull());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(created.getId());
    }

    @Test
    void rejectsUnknownCategory() {
        assertThatThrownBy(() -> controller.refresh("not-a-category"))
                .isInstanceOf(ApiException.class);
    }
}

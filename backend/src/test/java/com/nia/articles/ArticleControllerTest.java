package com.nia.articles;

import com.nia.auth.UserContext;
import com.nia.common.ApiException;
import com.nia.common.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pagination arguments arrive straight from the query string, so they are
 * attacker-controlled. A negative page used to be passed through untouched and
 * blew up inside PageRequest/subList as an HTTP 500; these tests pin the
 * clamping that turns such input into a harmless first page.
 */
class ArticleControllerTest {

    private final ArticleService articleService = mock(ArticleService.class);
    private final UserContext userContext = mock(UserContext.class);
    private final String userId = UUID.randomUUID().toString();

    private ArticleController controller;

    @BeforeEach
    void setUp() {
        controller = new ArticleController(articleService, userContext);
        when(userContext.requireUserId()).thenReturn(userId);
        PageResponse<ArticleDto> empty = PageResponse.of(List.of(), 0, 20, 0, false);
        when(articleService.getFeed(anyString(), anyInt(), anyInt())).thenReturn(empty);
        when(articleService.getByCategory(anyString(), anyString(), anyInt(), anyInt())).thenReturn(empty);
        when(articleService.searchKeyword(anyString(), anyString(), anyInt(), anyInt())).thenReturn(empty);
        when(articleService.searchSemantic(anyString(), anyString(), anyInt(), anyInt())).thenReturn(empty);
    }

    private ArgumentCaptor<Integer> pageOf(Runnable call, String method) {
        ArgumentCaptor<Integer> page = ArgumentCaptor.forClass(Integer.class);
        call.run();
        switch (method) {
            case "feed" -> verify(articleService).getFeed(eq(userId), page.capture(), anyInt());
            case "category" -> verify(articleService).getByCategory(eq(userId), anyString(), page.capture(), anyInt());
            case "keyword" -> verify(articleService).searchKeyword(eq(userId), anyString(), page.capture(), anyInt());
            case "semantic" -> verify(articleService).searchSemantic(eq(userId), anyString(), page.capture(), anyInt());
            default -> throw new IllegalArgumentException(method);
        }
        return page;
    }

    // ---- negative page must never reach the service layer ----

    @Test
    void clampsNegativePageOnFeed() {
        assertThat(pageOf(() -> controller.list(null, -1, 20), "feed").getValue()).isZero();
    }

    @Test
    void clampsNegativePageOnCategoryQueryParam() {
        assertThat(pageOf(() -> controller.list("technology", -7, 20), "category").getValue()).isZero();
    }

    @Test
    void clampsNegativePageOnCategoryPath() {
        assertThat(pageOf(() -> controller.byCategory("technology", -3, 20), "category").getValue()).isZero();
    }

    @Test
    void clampsNegativePageOnKeywordSearch() {
        assertThat(pageOf(() -> controller.search("india", "keyword", -2, 20), "keyword").getValue()).isZero();
    }

    @Test
    void clampsNegativePageOnSemanticSearch() {
        assertThat(pageOf(() -> controller.search("india", "semantic", -2, 20), "semantic").getValue()).isZero();
    }

    @Test
    void clampsIntegerMinValuePage() {
        assertThat(pageOf(() -> controller.list(null, Integer.MIN_VALUE, 20), "feed").getValue()).isZero();
    }

    // ---- size clamping (existing behaviour, pinned so the fix can't regress it) ----

    @Test
    void clampsOversizedPageSizeToTheMaximum() {
        ArgumentCaptor<Integer> size = ArgumentCaptor.forClass(Integer.class);
        controller.list(null, 0, 99999);
        verify(articleService).getFeed(eq(userId), anyInt(), size.capture());
        assertThat(size.getValue()).isEqualTo(50);
    }

    @Test
    void clampsNonPositivePageSizeToOne() {
        ArgumentCaptor<Integer> size = ArgumentCaptor.forClass(Integer.class);
        controller.list(null, 0, -5);
        verify(articleService).getFeed(eq(userId), anyInt(), size.capture());
        assertThat(size.getValue()).isEqualTo(1);
    }

    @Test
    void keepsAValidPageUnchanged() {
        assertThat(pageOf(() -> controller.list(null, 3, 20), "feed").getValue()).isEqualTo(3);
    }

    // ---- search input validation ----

    @Test
    void rejectsBlankSearchQuery() {
        assertThatThrownBy(() -> controller.search("   ", "keyword", 0, 20))
                .isInstanceOf(ApiException.class);
        verify(articleService, org.mockito.Mockito.never())
                .searchKeyword(anyString(), anyString(), anyInt(), anyInt());
    }

    @Test
    void requiresAuthenticationBeforeTouchingTheService() {
        when(userContext.requireUserId()).thenThrow(ApiException.unauthorized());
        assertThatThrownBy(() -> controller.list(null, 0, 20)).isInstanceOf(ApiException.class);
        verify(articleService, org.mockito.Mockito.never()).getFeed(any(), anyInt(), anyInt());
    }
}

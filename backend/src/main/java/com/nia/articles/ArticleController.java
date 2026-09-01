package com.nia.articles;

import com.nia.auth.UserContext;
import com.nia.common.ApiException;
import com.nia.common.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Read-side article endpoints: feed, category browse, detail, and search. */
@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private static final int MAX_SIZE = 50;

    private final ArticleService articleService;
    private final UserContext userContext;

    public ArticleController(ArticleService articleService, UserContext userContext) {
        this.articleService = articleService;
        this.userContext = userContext;
    }

    /** Personalized feed when no category is given; otherwise a category listing. */
    @GetMapping
    public PageResponse<ArticleDto> list(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String userId = userContext.requireUserId();
        if (category != null && !category.isBlank()) {
            return articleService.getByCategory(userId, category, clampPage(page), clampSize(size));
        }
        return articleService.getFeed(userId, clampPage(page), clampSize(size));
    }

    @GetMapping("/category/{category}")
    public PageResponse<ArticleDto> byCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return articleService.getByCategory(userContext.requireUserId(), category, clampPage(page), clampSize(size));
    }

    @GetMapping("/search")
    public PageResponse<ArticleDto> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "keyword") String mode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String userId = userContext.requireUserId();
        if (q == null || q.isBlank()) {
            throw ApiException.badRequest("Please enter something to search for.");
        }
        return "semantic".equalsIgnoreCase(mode)
                ? articleService.searchSemantic(userId, q, clampPage(page), clampSize(size))
                : articleService.searchKeyword(userId, q, clampPage(page), clampSize(size));
    }

    @GetMapping("/{id}")
    public ArticleDto byId(@PathVariable UUID id) {
        return articleService.getById(userContext.requireUserId(), id);
    }

    private int clampSize(int size) {
        if (size < 1) return 1;
        return Math.min(size, MAX_SIZE);
    }

    /**
     * A negative page is meaningless, and passing one straight through threw out
     * of PageRequest/subList as a 500. Clamp it to the first page, matching how an
     * out-of-range size is already handled.
     */
    private int clampPage(int page) {
        return Math.max(page, 0);
    }
}

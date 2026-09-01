package com.nia.news;

import com.nia.common.ApiException;
import com.nia.common.RateLimiter;
import com.nia.config.NiaProperties;
import com.nia.operations.OperationDto;
import com.nia.operations.OperationService;
import com.nia.operations.model.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

import com.nia.auth.UserContext;

/**
 * Manual news refresh. Starts a backend-owned operation and returns its id
 * immediately — the HTTP request is never held open for the whole ingestion,
 * so the work continues no matter what the browser does.
 */
@RestController
@RequestMapping("/api/news")
public class NewsController {

    public static final String OP_NEWS_REFRESH = "NEWS_REFRESH";

    private final OperationService operationService;
    private final NewsOperationRunner runner;
    private final CategoryMapper categoryMapper;
    private final UserContext userContext;
    private final RateLimiter rateLimiter;
    private final int refreshPerHour;

    public NewsController(OperationService operationService, NewsOperationRunner runner,
                          CategoryMapper categoryMapper, UserContext userContext,
                          RateLimiter rateLimiter, NiaProperties props) {
        this.operationService = operationService;
        this.runner = runner;
        this.categoryMapper = categoryMapper;
        this.userContext = userContext;
        this.rateLimiter = rateLimiter;
        this.refreshPerHour = props.getRateLimit().getRefreshPerHour();
    }

    @PostMapping("/refresh")
    public ResponseEntity<OperationDto> refresh(@RequestParam(required = false) String category) {
        UUID userId = UUID.fromString(userContext.requireUserId());

        if (category != null && !category.isBlank() && !categoryMapper.isValid(category)) {
            throw ApiException.badRequest("Unknown category.");
        }

        // Duplicate protection: reconnect to an in-flight refresh instead of starting another.
        Optional<OperationDto> active = operationService.findActiveForUser(userId, OP_NEWS_REFRESH);
        if (active.isPresent()) {
            return ResponseEntity.ok(active.get());
        }

        // Only spend a rate-limit token when actually starting new work.
        if (!rateLimiter.tryAcquire(userId.toString(), "news_refresh", refreshPerHour)) {
            throw ApiException.rateLimited("You've refreshed a lot recently — please try again shortly.");
        }

        Operation operation = operationService.create(userId, OP_NEWS_REFRESH);
        runner.run(operation.getId(), category); // async — returns immediately
        return ResponseEntity.accepted().body(operationService.getForUser(operation.getId(), userId));
    }
}

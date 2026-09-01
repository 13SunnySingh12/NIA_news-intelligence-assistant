package com.nia.bookmarks;

import com.nia.articles.ArticleDto;
import com.nia.auth.UserContext;
import com.nia.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/bookmarks")
public class BookmarkController {

    private final BookmarkService bookmarkService;
    private final UserContext userContext;

    public BookmarkController(BookmarkService bookmarkService, UserContext userContext) {
        this.bookmarkService = bookmarkService;
        this.userContext = userContext;
    }

    @GetMapping
    public PageResponse<ArticleDto> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // A negative page is meaningless and threw out of PageRequest as a 500.
        return bookmarkService.list(userContext.requireUserId(),
                Math.max(page, 0), Math.min(Math.max(size, 1), 50));
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> add(@Valid @RequestBody BookmarkRequest request) {
        bookmarkService.add(userContext.requireUserId(), request.articleId());
        return ResponseEntity.ok(Map.of("status", "bookmarked"));
    }

    @DeleteMapping("/{articleId}")
    public ResponseEntity<Map<String, String>> remove(@PathVariable UUID articleId) {
        bookmarkService.remove(userContext.requireUserId(), articleId);
        return ResponseEntity.ok(Map.of("status", "removed"));
    }
}

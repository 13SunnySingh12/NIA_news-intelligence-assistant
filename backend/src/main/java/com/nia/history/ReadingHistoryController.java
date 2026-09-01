package com.nia.history;

import com.nia.auth.UserContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/** Logs that the current user opened an article. */
@RestController
@RequestMapping("/api/articles")
public class ReadingHistoryController {

    private final ReadingHistoryService readingHistoryService;
    private final UserContext userContext;

    public ReadingHistoryController(ReadingHistoryService readingHistoryService, UserContext userContext) {
        this.readingHistoryService = readingHistoryService;
        this.userContext = userContext;
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Map<String, String>> markRead(@PathVariable UUID id) {
        readingHistoryService.recordRead(userContext.requireUserId(), id);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}

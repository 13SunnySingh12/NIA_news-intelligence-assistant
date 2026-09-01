package com.nia.trending;

import com.nia.articles.ArticleDto;
import com.nia.auth.UserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trending")
public class TrendingController {

    private final TrendingService trendingService;
    private final UserContext userContext;

    public TrendingController(TrendingService trendingService, UserContext userContext) {
        this.trendingService = trendingService;
        this.userContext = userContext;
    }

    @GetMapping
    public List<ArticleDto> trending(@RequestParam(required = false) String category) {
        return trendingService.getTrending(userContext.requireUserId(), category);
    }
}

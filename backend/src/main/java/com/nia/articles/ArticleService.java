package com.nia.articles;

import com.nia.articles.model.Article;
import com.nia.assistant.AssistantClient;
import com.nia.assistant.dto.EmbedRequest;
import com.nia.assistant.dto.SearchRequest;
import com.nia.assistant.dto.SearchResponse;
import com.nia.bookmarks.BookmarkRepository;
import com.nia.common.ApiException;
import com.nia.common.PageResponse;
import com.nia.news.CategoryMapper;
import com.nia.news.CategoryValidator;
import com.nia.news.ContentFingerprint;
import com.nia.news.NormalizedArticle;
import com.nia.news.UrlCanonicalizer;
import com.nia.personalization.PersonalizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Reads and serves articles; also persists newly-ingested articles and drives search. */
@Service
public class ArticleService {

    private static final Logger log = LoggerFactory.getLogger(ArticleService.class);
    private static final int SEMANTIC_TOP_K = 20;
    /** Cap on AI classification calls per ingestion pass (LLM free-tier friendly). */
    private static final int CLASSIFY_MAX_PER_PASS = 20;

    private final ArticleRepository articleRepository;
    private final BookmarkRepository bookmarkRepository;
    private final PersonalizationService personalizationService;
    private final AssistantClient assistantClient;
    private final CategoryMapper categoryMapper;
    private final CategoryValidator categoryValidator;

    public ArticleService(ArticleRepository articleRepository, BookmarkRepository bookmarkRepository,
                          PersonalizationService personalizationService, AssistantClient assistantClient,
                          CategoryMapper categoryMapper, CategoryValidator categoryValidator) {
        this.articleRepository = articleRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.personalizationService = personalizationService;
        this.assistantClient = assistantClient;
        this.categoryMapper = categoryMapper;
        this.categoryValidator = categoryValidator;
    }

    /** The personalized home feed. */
    public PageResponse<ArticleDto> getFeed(String userId, int page, int size) {
        UUID uid = UUID.fromString(userId);
        List<Article> ranked = personalizationService.personalizedFeed(uid);
        int from = Math.min(page * size, ranked.size());
        int to = Math.min(from + size, ranked.size());
        Set<UUID> bookmarked = bookmarkedIds(uid);
        List<ArticleDto> content = new ArrayList<>();
        for (Article a : ranked.subList(from, to)) {
            content.add(ArticleDto.of(a, false, bookmarked.contains(a.getId())));
        }
        return PageResponse.of(content, page, size, ranked.size(), to < ranked.size());
    }

    public PageResponse<ArticleDto> getByCategory(String userId, String category, int page, int size) {
        if (!categoryMapper.isValid(category)) {
            throw ApiException.badRequest("Unknown category.");
        }
        UUID uid = UUID.fromString(userId);
        Page<Article> result = articleRepository.findByCategoryOrderByPublishedAtDesc(
                category.toLowerCase(), PageRequest.of(page, size));
        return toPage(result, uid, false);
    }

    public ArticleDto getById(String userId, UUID id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("That article couldn't be found."));
        boolean bookmarked = bookmarkRepository.existsByIdUserIdAndIdArticleId(UUID.fromString(userId), id);
        return ArticleDto.of(article, true, bookmarked);
    }

    public PageResponse<ArticleDto> searchKeyword(String userId, String query, int page, int size) {
        UUID uid = UUID.fromString(userId);
        Page<Article> result = articleRepository.searchByKeyword(query.trim(), PageRequest.of(page, size));
        return toPage(result, uid, false);
    }

    /** Semantic search via FastAPI; falls back to keyword search if the AI service is unavailable. */
    public PageResponse<ArticleDto> searchSemantic(String userId, String query, int page, int size) {
        UUID uid = UUID.fromString(userId);
        SearchResponse response;
        try {
            response = assistantClient.search(new SearchRequest(query.trim(), SEMANTIC_TOP_K, null));
        } catch (AssistantClient.AiUnavailableException ex) {
            log.info("Falling back to keyword search for query.");
            return searchKeyword(userId, query, page, size);
        }
        if (response == null || response.results() == null || response.results().isEmpty()) {
            return PageResponse.of(List.of(), page, size, 0, false);
        }

        List<UUID> ids = response.results().stream().map(SearchResponse.SearchHit::id).toList();
        Map<UUID, Article> byId = new LinkedHashMap<>();
        for (Article a : articleRepository.findByIdIn(ids)) {
            byId.put(a.getId(), a);
        }
        List<Article> ordered = new ArrayList<>();
        for (UUID id : ids) {
            Article a = byId.get(id);
            if (a != null) ordered.add(a);
        }

        int from = Math.min(page * size, ordered.size());
        int to = Math.min(from + size, ordered.size());
        Set<UUID> bookmarked = bookmarkedIds(uid);
        List<ArticleDto> content = new ArrayList<>();
        for (Article a : ordered.subList(from, to)) {
            content.add(ArticleDto.of(a, false, bookmarked.contains(a.getId())));
        }
        return PageResponse.of(content, page, size, ordered.size(), to < ordered.size());
    }

    /**
     * Persist newly-aggregated articles that aren't already stored, and return the
     * text of each newly-saved article so the caller can request embeddings.
     */
    public List<EmbedRequest.EmbedItem> ingest(List<NormalizedArticle> articles) {
        // 1) In-cycle dedup: keep the first valid article per URL and per story fingerprint.
        List<NormalizedArticle> candidates = new ArrayList<>();
        List<String> canonicals = new ArrayList<>();
        List<String> fingerprints = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();
        Set<String> seenHashes = new HashSet<>();
        for (NormalizedArticle na : articles) {
            if (na.title() == null || na.url() == null || na.publishedAt() == null) {
                continue;
            }
            String canonical = UrlCanonicalizer.canonicalize(na.url());
            if (canonical.isBlank()) {
                continue;
            }
            String fingerprint = ContentFingerprint.of(na.title(), na.publishedAt());
            if (!seenUrls.add(canonical) || !seenHashes.add(fingerprint)) {
                continue;
            }
            candidates.add(na);
            canonicals.add(canonical);
            fingerprints.add(fingerprint);
        }
        if (candidates.isEmpty()) {
            return new ArrayList<>();
        }

        // 2) Cross-cycle / cross-provider dedup in TWO batched queries (not two per article).
        Set<String> existingUrls = new HashSet<>(articleRepository.findExistingCanonicalUrls(canonicals));
        Set<String> existingHashes = new HashSet<>(articleRepository.findExistingContentHashes(fingerprints));

        // 3) Persist the genuinely new articles.
        // Every article is checked against the category it was fetched under: a
        // provider query for "india" routinely returns global filler, so the fetched
        // label is not evidence on its own. The check is deterministic and in-memory,
        // so it adds no network cost: clear mismatches are dropped, ambiguous ones
        // keep the provider's label. Only articles with NO usable category at all
        // fall through to the AI classifier, which stays capped per pass.
        int classifyBudget = CLASSIFY_MAX_PER_PASS;
        int rejected = 0;
        List<EmbedRequest.EmbedItem> newlySaved = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            if (existingUrls.contains(canonicals.get(i)) || existingHashes.contains(fingerprints.get(i))) {
                continue;
            }
            try {
                Article entity = toEntity(candidates.get(i), canonicals.get(i), fingerprints.get(i));

                String claimed = entity.getCategory();
                if (claimed != null) {
                    CategoryValidator.Verdict verdict = categoryValidator.validate(candidates.get(i), claimed);
                    if (verdict == CategoryValidator.Verdict.REJECT) {
                        // Better to show fewer articles than wrong ones.
                        rejected++;
                        continue;
                    }
                    // UNCERTAIN keeps the provider's category: it is no worse than
                    // before, and calling the LLM here would put a network round trip
                    // in the ingestion loop for every ambiguous article.
                }
                if (entity.getCategory() == null) {
                    entity.setCategory(classifyOrDefault(candidates.get(i), classifyBudget > 0));
                    classifyBudget--;
                }
                Article saved = articleRepository.save(entity);
                newlySaved.add(new EmbedRequest.EmbedItem(saved.getId(), embedText(candidates.get(i))));
            } catch (DataIntegrityViolationException ex) {
                // A concurrent cycle inserted the same URL first — the DB unique constraint
                // caught it, so just skip. No duplicate is stored.
                log.debug("Skipping duplicate on insert: {}", canonicals.get(i));
            }
        }
        if (rejected > 0) {
            log.info("Category validation rejected {} article(s) that did not match their category", rejected);
        }
        return newlySaved;
    }

    private Article toEntity(NormalizedArticle na, String canonical, String contentHash) {
        Article a = new Article();
        a.setId(na.id());
        a.setTitle(na.title());
        a.setDescription(na.description());
        a.setUrl(na.url());
        a.setCanonicalUrl(canonical);
        a.setSource(na.source() != null ? na.source() : "Unknown");
        a.setAuthor(na.author());
        // null here means "couldn't map confidently" — the caller then asks the AI service.
        a.setCategory(categoryMapper.isValid(na.category()) ? na.category().toLowerCase()
                : categoryMapper.toNiaOrNull(na.category()));
        a.setLanguage(na.language() != null ? na.language() : "en");
        a.setCountry(na.country());
        a.setImageUrl(na.imageUrl());
        a.setContent(na.content());
        a.setPublishedAt(na.publishedAt());
        a.setProvider(na.provider() != null ? na.provider() : "unknown");
        a.setContentHash(contentHash);
        return a;
    }

    /**
     * Ask the AI service (Groq) for a category when the provider's token couldn't be
     * mapped. Falls back to "world" when the budget is spent or the AI is unavailable.
     */
    private String classifyOrDefault(NormalizedArticle na, boolean withinBudget) {
        if (withinBudget) {
            String category = assistantClient.classify(na.title(), na.description());
            if (categoryMapper.isValid(category)) {
                return category.toLowerCase();
            }
        }
        return "world";
    }

    /** Embedding input: title + description (or a content excerpt), per NIA's embedding rule. */
    private String embedText(NormalizedArticle na) {
        String body = na.description();
        if ((body == null || body.isBlank()) && na.content() != null) {
            body = na.content().length() > 1000 ? na.content().substring(0, 1000) : na.content();
        }
        return na.title() + (body != null ? "\n" + body : "");
    }

    private PageResponse<ArticleDto> toPage(Page<Article> page, UUID userId, boolean includeContent) {
        Set<UUID> bookmarked = bookmarkedIds(userId);
        List<ArticleDto> content = new ArrayList<>();
        for (Article a : page.getContent()) {
            content.add(ArticleDto.of(a, includeContent, bookmarked.contains(a.getId())));
        }
        return PageResponse.from(page, content);
    }

    private Set<UUID> bookmarkedIds(UUID userId) {
        return new HashSet<>(bookmarkRepository.findArticleIdsByUser(userId));
    }
}

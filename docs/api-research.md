# News Provider Notes

> Free tiers and endpoints change. Re-check the official docs before relying on
> any limit below. Verified for this build in August 2026.

## Selected providers (priority order)

| Provider | Role | Endpoint used | Notes |
|---|---|---|---|
| GNews | Primary | `GET https://gnews.io/api/v4/top-headlines` and `/search` | `category`, `lang`, `country`, `max`, `apikey`. ~100 req/day free. |
| NewsData.io | Primary | `GET https://newsdata.io/api/1/latest` | `apikey`, `language`, `category`, `country`, `q`. Free returns ~10 articles/request. |
| The Guardian | Secondary | `GET https://content.guardianapis.com/search` | Real full text via `show-fields=trailText,bodyText,thumbnail,byline`. Generous non-commercial limit. |
| Currents | Fallback | `/v1/latest-news`, `/v1/search` | `language`, `category`, `apiKey`. |
| Google News RSS | Fallback | `https://news.google.com/rss/search` | No key. Titles/links/summaries only (no full text). |
| NewsAPI | Dev only | `/v2/top-headlines`, `/v2/everything` | Free plan forbids production; enabled only under the `dev` Spring profile. |

## Design choices

- Every provider implements one `NewsProvider` interface and maps its response
  onto `NormalizedArticle`. Adding a provider is a new class + a config entry.
- The aggregator tries providers in priority order until it has enough articles,
  skipping any near its per-provider daily soft cap.
- All external data is treated as untrusted: missing images, null fields, and
  odd date formats are handled defensively and never break ingestion.

## Getting keys

- GNews — https://gnews.io (free key on signup)
- NewsData.io — https://newsdata.io
- The Guardian — https://open-platform.theguardian.com/access/ (`test` key works for trials)
- Currents — https://currentsapi.services
- Google News RSS — no key required

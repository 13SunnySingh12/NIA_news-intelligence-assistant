from app.rag.postprocess import map_sources


def _retrieved():
    return [
        {"id": "a1", "title": "First", "source": "GNews", "url": "https://x/1"},
        {"id": "a2", "title": "Second", "source": "Guardian", "url": "https://x/2"},
        {"id": "a3", "title": "Third", "source": "NewsData", "url": "https://x/3"},
    ]


def test_maps_cited_articles_in_appearance_order():
    answer = "As reported in [2] and later [1], the story developed."
    sources = map_sources(answer, _retrieved())
    assert [s["id"] for s in sources] == ["a2", "a1"]


def test_falls_back_to_all_when_no_citations():
    sources = map_sources("A plain answer with no markers.", _retrieved())
    assert len(sources) == 3


def test_drops_out_of_range_citations():
    sources = map_sources("Nonsense [9] citation.", _retrieved())
    # [9] doesn't map to anything, so fall back to all retrieved.
    assert len(sources) == 3


def test_empty_retrieval_returns_no_sources():
    assert map_sources("[1]", []) == []

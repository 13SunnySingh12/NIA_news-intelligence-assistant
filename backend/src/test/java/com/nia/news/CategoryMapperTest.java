package com.nia.news;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryMapperTest {

    private final CategoryMapper mapper = new CategoryMapper();

    @Test
    void mapsKnownSynonyms() {
        assertThat(mapper.toNia("tech")).isEqualTo("technology");
        assertThat(mapper.toNia("finance")).isEqualTo("business");
        assertThat(mapper.toNia("sport")).isEqualTo("sports");
        assertThat(mapper.toNia("general")).isEqualTo("world");
    }

    @Test
    void defaultsUnknownToWorld() {
        assertThat(mapper.toNia("random-nonsense")).isEqualTo("world");
        assertThat(mapper.toNia(null)).isEqualTo("world");
    }

    @Test
    void validatesCategories() {
        assertThat(mapper.isValid("technology")).isTrue();
        assertThat(mapper.isValid("TECHNOLOGY")).isTrue();
        assertThat(mapper.isValid("not-a-category")).isFalse();
        assertThat(mapper.isValid(null)).isFalse();
    }
}

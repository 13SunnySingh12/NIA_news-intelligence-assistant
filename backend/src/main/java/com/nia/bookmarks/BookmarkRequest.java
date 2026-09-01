package com.nia.bookmarks;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Body for POST /api/bookmarks. */
public record BookmarkRequest(@NotNull UUID articleId) {
}

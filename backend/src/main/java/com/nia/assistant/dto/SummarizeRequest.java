package com.nia.assistant.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

/** Request an on-demand summary of an article, either "short" or "detailed". */
public record SummarizeRequest(
        @NotNull UUID articleId,
        @Pattern(regexp = "short|detailed", message = "length must be 'short' or 'detailed'") String length
) {
}

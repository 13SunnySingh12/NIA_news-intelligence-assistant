package com.nia.common;

import org.springframework.data.domain.Page;

import java.util.List;

/** Simple, frontend-friendly pagination envelope. */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        boolean hasNext
) {
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements, boolean hasNext) {
        return new PageResponse<>(content, page, size, totalElements, hasNext);
    }

    /** Build from a Spring Data page whose entities were mapped to {@code content}. */
    public static <T> PageResponse<T> from(Page<?> page, List<T> content) {
        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.hasNext());
    }
}

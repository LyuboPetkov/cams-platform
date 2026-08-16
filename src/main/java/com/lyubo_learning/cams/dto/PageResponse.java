package com.lyubo_learning.cams.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

// A hand-written page envelope rather than Spring Data's Page/PageImpl. Spring
// Data does not guarantee PageImpl's JSON structure across versions, and every
// other response type in this codebase is a hand-written DTO with a documented
// schema — so this stays consistent and gives springdoc something stable.
@Getter
@Builder
@AllArgsConstructor
@Schema(description = "One page of results, with the paging metadata needed to walk the rest")
public class PageResponse<T> {

    @Schema(description = "The results on this page")
    private List<T> content;

    @Schema(description = "Index of this page, 0-based", example = "0")
    private int page;

    @Schema(description = "Number of results requested per page", example = "20")
    private int size;

    @Schema(description = "Total number of results across all pages", example = "137")
    private long totalElements;

    @Schema(description = "Total number of pages available", example = "7")
    private int totalPages;

    @Schema(description = "Whether this is the first page")
    private boolean first;

    @Schema(description = "Whether this is the last page")
    private boolean last;

    // Takes the already-mapped content rather than mapping anything itself —
    // the page carries entities, the response carries DTOs.
    public static <T> PageResponse<T> of(Page<?> page, List<T> content) {
        return PageResponse.<T>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}

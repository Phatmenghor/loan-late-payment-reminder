package com.backend.shared.mapper;

import com.backend.shared.dto.PaginationResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Component
public class PaginationMapper {

    /**
     * Universal pagination mapper with function-based transformation
     */
    public <T, R> PaginationResponse<R> toPaginationResponse(Page<T> page, Function<List<T>, List<R>> mapper) {
        List<R> content = mapper.apply(page.getContent());
        return PaginationResponse.<R>builder()
                .content(content)
                .pageNo(page.getNumber() + 1)
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    /**
     * Universal pagination mapper with pre-mapped content
     */
    public <T, R> PaginationResponse<R> toPaginationResponse(Page<T> page, List<R> mappedContent) {
        return PaginationResponse.<R>builder()
                .content(mappedContent)
                .pageNo(page.getNumber() + 1)
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    /**
     * Simple pagination mapper for direct entity-to-response mapping
     */
    public <T> PaginationResponse<T> toPaginationResponse(Page<T> page) {
        return PaginationResponse.<T>builder()
                .content(page.getContent())
                .pageNo(page.getNumber() + 1)
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }
}

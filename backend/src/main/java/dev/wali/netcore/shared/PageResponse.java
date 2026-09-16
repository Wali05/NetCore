package dev.wali.netcore.shared;

import org.springframework.data.domain.Page;

import java.util.function.Function;
import java.util.List;

public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static <S, T> PageResponse<T> from(
            Page<S> source,
            Function<S, T> mapper
    ) {
        return new PageResponse<>(
                source.getContent().stream().map(mapper).toList(),
                source.getNumber(),
                source.getSize(),
                source.getTotalElements(),
                source.getTotalPages()
        );
    }
}

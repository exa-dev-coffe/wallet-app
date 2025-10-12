package com.wallet_service.be.utils.commons;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@Builder
public class PaginationResponseDto<T> {
    private List<T> data;
    private long totalData;
    private int totalPages;
    private int currentPage;
    private int pageSize;
    private boolean isLastPage;

    public static <T> PaginationResponseDto<T> fromEntity(Page<T> entity) {
        return PaginationResponseDto.<T>builder()
                .data(entity.getContent())
                .totalData(entity.getTotalElements())
                .totalPages(entity.getTotalPages())
                .currentPage(entity.getNumber() + 1) // Page number starts from 0
                .pageSize(entity.getSize())
                .isLastPage(entity.isLast())
                .build();
    }

}

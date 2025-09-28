package com.time_tracker.be.utils.commons;

import lombok.Data;

import java.util.List;

@Data
public class PaginationResponseDto<T> {
    private List<T> data;
    private long totalData;
    private int totalPages;
    private int currentPage;
    private int pageSize;
    private boolean isLastPage;

}

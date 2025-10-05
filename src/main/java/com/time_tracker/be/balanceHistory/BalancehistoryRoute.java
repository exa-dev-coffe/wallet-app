package com.time_tracker.be.balanceHistory;

import com.time_tracker.be.annotation.CurrentUser;
import com.time_tracker.be.annotation.RequireAuth;
import com.time_tracker.be.balanceHistory.dto.BalanceHistoryResponseDto;
import com.time_tracker.be.utils.commons.CurrentUserDto;
import com.time_tracker.be.utils.commons.PaginationResponseDto;
import com.time_tracker.be.utils.commons.ResponseModel;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/1.0")
public class BalancehistoryRoute {
    private final BalancehistoryService balancehistoryService;

    public BalancehistoryRoute(BalancehistoryService balancehistoryService) {
        this.balancehistoryService = balancehistoryService;
    }

    @GetMapping("/balance-history")
    @RequireAuth
    public ResponseEntity<ResponseModel<PaginationResponseDto<BalanceHistoryResponseDto>>> getAllBalanceHistory(
            Pageable pageable,
            @Param("searchValue") String searchValue,
            @Param("searchKey") String searchKey,
            @CurrentUser CurrentUserDto currentUser
    ) {
        // Kurangi 1, pastikan tidak negatif
        int pageNumber = pageable.getPageNumber() > 0 ? pageable.getPageNumber() - 1 : 0;

        // Buat Pageable baru
        Pageable adjustedPageable = PageRequest.of(pageNumber, pageable.getPageSize(), pageable.getSort());

        // Panggil service pakai pageable yang sudah di-adjust
        return balancehistoryService.getAllBalanceHistory(adjustedPageable, searchValue, searchKey, currentUser.getUserId());
    }

}

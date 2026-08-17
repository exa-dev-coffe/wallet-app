package com.wallet_service.be.balanceHistory;

import com.wallet_service.be.annotation.CurrentUser;
import com.wallet_service.be.annotation.RequireAuth;
import com.wallet_service.be.balanceHistory.dto.BalanceHistoryResponseDto;
import com.wallet_service.be.utils.commons.CurrentUserDto;
import com.wallet_service.be.utils.commons.PaginationResponseDto;
import com.wallet_service.be.utils.commons.ResponseModel;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import java.util.UUID;

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

    @GetMapping("/balance-history/{id}")
    @RequireAuth
    public ResponseEntity<ResponseModel<BalanceHistoryResponseDto>> getBalanceHistoryDetail(
            @PathVariable("id") UUID id,
            @CurrentUser CurrentUserDto currentUser
    ) {
        return balancehistoryService.getBalanceHistoryDetail(id, currentUser.getUserId());
    }

}


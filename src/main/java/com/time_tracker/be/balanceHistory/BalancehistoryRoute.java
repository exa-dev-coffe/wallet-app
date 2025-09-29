package com.time_tracker.be.balanceHistory;

import com.time_tracker.be.balanceHistory.dto.BalanceHistoryResponseDto;
import com.time_tracker.be.utils.commons.PaginationResponseDto;
import com.time_tracker.be.utils.commons.ResponseModel;
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
    public ResponseEntity<ResponseModel<PaginationResponseDto<BalanceHistoryResponseDto>>> getAllBalanceHistory(Pageable pageable, @Param("searchValue") String searchValue, @Param("searchKey") String searchKey) {
        return balancehistoryService.getAllBalanceHistory(pageable, searchValue, searchKey);
    }

}

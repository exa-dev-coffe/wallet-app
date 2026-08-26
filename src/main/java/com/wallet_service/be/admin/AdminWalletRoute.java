package com.wallet_service.be.admin;

import com.wallet_service.be.admin.dto.AdminResetPinRequestDto;
import com.wallet_service.be.admin.dto.AdminSendResetPinRequestDto;
import com.wallet_service.be.admin.dto.AdminWalletResponseDto;
import com.wallet_service.be.admin.dto.AdminWalletSummaryDto;
import com.wallet_service.be.annotation.ActionType;
import com.wallet_service.be.annotation.RequireAuth;
import com.wallet_service.be.annotation.RequirePermission;
import com.wallet_service.be.balanceHistory.BalancehistoryService;
import com.wallet_service.be.balanceHistory.dto.BalanceHistoryResponseDto;
import com.wallet_service.be.utils.commons.PaginationResponseDto;
import com.wallet_service.be.utils.commons.ResponseModel;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/1.0/admin/wallets")
public class AdminWalletRoute {

    private final AdminWalletService adminWalletService;
    private final BalancehistoryService balancehistoryService;

    public AdminWalletRoute(AdminWalletService adminWalletService, BalancehistoryService balancehistoryService) {
        this.adminWalletService = adminWalletService;
        this.balancehistoryService = balancehistoryService;
    }

    @GetMapping
    @RequireAuth
    @RequirePermission(feature = "wallet_management", action = ActionType.VIEW)
    public ResponseEntity<ResponseModel<PaginationResponseDto<AdminWalletResponseDto>>> getAllWallets(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sort", defaultValue = "createdAt,desc") String sort,
            @RequestParam(name = "search", required = false) String search
    ) {
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        Sort.Direction direction = (sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, Sort.by(direction, sortField));
        return adminWalletService.getAllWallets(pageable, search);
    }

    @GetMapping("/summary")
    @RequireAuth
    @RequirePermission(feature = "wallet_management", action = ActionType.VIEW)
    public ResponseEntity<ResponseModel<AdminWalletSummaryDto>> getWalletSummary() {
        return adminWalletService.getWalletSummary();
    }

    @GetMapping("/{userId}/history")
    @RequireAuth
    @RequirePermission(feature = "wallet_management", action = ActionType.VIEW)
    public ResponseEntity<ResponseModel<PaginationResponseDto<BalanceHistoryResponseDto>>> getWalletHistory(
            @PathVariable("userId") int userId,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sort", defaultValue = "createdAt,desc") String sort,
            @RequestParam(name = "searchKey", required = false) String searchKey,
            @RequestParam(name = "searchValue", required = false) String searchValue
    ) {
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        Sort.Direction direction = (sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, Sort.by(direction, sortField));
        return balancehistoryService.getAllBalanceHistory(pageable, searchValue, searchKey, userId);
    }

    @PostMapping("/reset-pin/send-code")
    @RequireAuth
    @RequirePermission(feature = "wallet_management", action = ActionType.EDIT)
    public ResponseEntity<ResponseModel<String>> sendResetPinCode(@Valid @RequestBody AdminSendResetPinRequestDto request) {
        return adminWalletService.sendResetPinCodeToEmail(request);
    }

    @PostMapping("/reset-pin")
    @RequireAuth
    @RequirePermission(feature = "wallet_management", action = ActionType.EDIT)
    public ResponseEntity<ResponseModel<String>> resetCustomerPin(@Valid @RequestBody AdminResetPinRequestDto request) {
        return adminWalletService.resetCustomerPin(request);
    }

    @PostMapping("/{userId}/toggle-status")
    @RequireAuth
    @RequirePermission(feature = "wallet_management", action = ActionType.EDIT)
    public ResponseEntity<ResponseModel<String>> toggleWalletStatus(@PathVariable("userId") int userId) {
        return adminWalletService.toggleWalletStatus(userId);
    }
}

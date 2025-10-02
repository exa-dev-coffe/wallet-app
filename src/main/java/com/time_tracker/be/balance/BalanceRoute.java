package com.time_tracker.be.balance;

import com.time_tracker.be.annotation.CurrentUser;
import com.time_tracker.be.annotation.RequireAuth;
import com.time_tracker.be.balance.dto.GetBalanceResponseDto;
import com.time_tracker.be.balance.dto.SetPinRequestDto;
import com.time_tracker.be.balance.dto.TopUpRequestDto;
import com.time_tracker.be.balance.dto.TopUpResponseDto;
import com.time_tracker.be.balanceHistory.dto.MidtransRequestDto;
import com.time_tracker.be.balanceHistory.enums.StatusBalanceHistory;
import com.time_tracker.be.lib.MidtransService;
import com.time_tracker.be.utils.commons.CurrentUserDto;
import com.time_tracker.be.utils.commons.ResponseModel;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/1.0")
public class BalanceRoute {
    private final BalanceService balanceService;
    private final MidtransService midtransService;

    public BalanceRoute(BalanceService balanceService, MidtransService midtransService) {
        this.midtransService = midtransService;
        this.balanceService = balanceService;
    }

    @GetMapping("/balance")
    @RequireAuth
    public ResponseEntity<ResponseModel<GetBalanceResponseDto>> getBalance(@CurrentUser() CurrentUserDto currentUserDto) {
        return balanceService.getBalanceByUserId(currentUserDto.getUserId());
    }

    @PostMapping("/balance/activate")
    @RequireAuth
    public ResponseEntity<ResponseModel<String>> setPin(@CurrentUser() CurrentUserDto currentUserDto, @Valid @RequestBody SetPinRequestDto setPinRequest) {
        return balanceService.setPin(currentUserDto.getUserId(), setPinRequest.getPin());
    }

    @PostMapping("/balance/top-up")
    @RequireAuth
    public ResponseEntity<ResponseModel<TopUpResponseDto>> topUpBalance(@CurrentUser() CurrentUserDto currentUserDto, @Valid @RequestBody TopUpRequestDto topUpRequestDto) throws Exception {
        return balanceService.topUp(currentUserDto.getUserId(), topUpRequestDto.getAmount(), topUpRequestDto.getPin(), currentUserDto.getEmail(), currentUserDto.getFullName());
    }

    @PostMapping("/midtrans-notification")
    public ResponseEntity<ResponseModel<String>> midtransNotificationHandler(@Valid @RequestBody MidtransRequestDto midtransRequestDto) throws Exception {
        StatusBalanceHistory status = midtransService.mapTransactionStatus(midtransRequestDto.getTransactionStatus(), midtransRequestDto.getFraudStatus());
        return balanceService.notificationMidtransHandler(midtransRequestDto.getOrderId(), status, midtransRequestDto.getStatusCode(), midtransRequestDto.getGrossAmount(), midtransRequestDto.getSignatureKey());
    }


}

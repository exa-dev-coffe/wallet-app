package com.wallet_service.be.balance;

import com.wallet_service.be.annotation.CurrentUser;
import com.wallet_service.be.annotation.RequireAuth;
import com.wallet_service.be.balance.dto.ChangePinRequestDto;
import com.wallet_service.be.balance.dto.GetBalanceResponseDto;
import com.wallet_service.be.balance.dto.ResetPinRequestDto;
import com.wallet_service.be.balance.dto.SetPinRequestDto;
import com.wallet_service.be.balance.dto.TopUpRequestDto;
import com.wallet_service.be.balance.dto.TopUpResponseDto;
import com.wallet_service.be.balanceHistory.dto.MidtransRequestDto;
import com.wallet_service.be.balanceHistory.enums.StatusBalanceHistory;
import com.wallet_service.be.lib.MidtransService;
import com.wallet_service.be.utils.commons.CurrentUserDto;
import com.wallet_service.be.utils.commons.ResponseModel;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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

    @PostMapping("/balance/change-pin")
    @RequireAuth
    public ResponseEntity<ResponseModel<String>> changePin(@CurrentUser() CurrentUserDto currentUserDto, @Valid @RequestBody ChangePinRequestDto request) {
        return balanceService.changePin(currentUserDto.getUserId(), request.getOldPin(), request.getNewPin());
    }

    @PostMapping("/balance/reset-pin/send-code")
    @RequireAuth
    public ResponseEntity<ResponseModel<String>> sendResetPinCode(@CurrentUser() CurrentUserDto currentUserDto) throws Exception {
        return balanceService.sendResetPinCode(currentUserDto.getUserId(), currentUserDto.getEmail());
    }

    @PostMapping("/balance/reset-pin")
    @RequireAuth
    public ResponseEntity<ResponseModel<String>> resetPin(@CurrentUser() CurrentUserDto currentUserDto, @Valid @RequestBody ResetPinRequestDto request) {
        return balanceService.resetPin(currentUserDto.getUserId(), currentUserDto.getEmail(), request.getCode(), request.getNewPin());
    }

    @PostMapping("/balance/top-up")
    @RequireAuth
    public ResponseEntity<ResponseModel<TopUpResponseDto>> topUpBalance(@CurrentUser() CurrentUserDto currentUserDto, @Valid @RequestBody TopUpRequestDto topUpRequestDto) throws Exception {
        return balanceService.topUp(currentUserDto.getUserId(), topUpRequestDto.getAmount(), topUpRequestDto.getPaymentType(), topUpRequestDto.getBank(), currentUserDto.getEmail(), currentUserDto.getFullName());
    }


    @PostMapping("/midtrans-notification")
    public ResponseEntity<ResponseModel<String>> midtransNotificationHandler(@Valid @RequestBody MidtransRequestDto midtransRequestDto) throws Exception {
        StatusBalanceHistory status = midtransService.mapTransactionStatus(midtransRequestDto.getTransactionStatus(), midtransRequestDto.getFraudStatus());
        return balanceService.notificationMidtransHandler(midtransRequestDto.getOrderId(), status, midtransRequestDto.getStatusCode(), midtransRequestDto.getGrossAmount(), midtransRequestDto.getSignatureKey());
    }

    @PostMapping("/balance/top-up/{id}/sync")
    @RequireAuth
    public ResponseEntity<ResponseModel<String>> syncTransactionStatus(@CurrentUser() CurrentUserDto currentUserDto, @PathVariable("id") UUID id) throws Exception {
        return balanceService.syncTransactionStatus(id, currentUserDto.getUserId());
    }

}

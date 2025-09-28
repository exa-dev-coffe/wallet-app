package com.time_tracker.be.balance;

import com.time_tracker.be.annotation.CurrentUser;
import com.time_tracker.be.balance.dto.BalanceResponseDto;
import com.time_tracker.be.balance.dto.SetPinRequestDto;
import com.time_tracker.be.utils.commons.CurrentUserDto;
import com.time_tracker.be.utils.commons.ResponseModel;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/1.0")
public class BalanceRoute {
    private final BalanceService balanceService;

    public BalanceRoute(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @GetMapping("/balance")
    public ResponseEntity<ResponseModel<BalanceResponseDto>> getBalance(@CurrentUser() CurrentUserDto currentUserDto) {
        return balanceService.getBalanceByUserId(currentUserDto.getUserId());
    }

    @PostMapping("/balance/activate")
    public ResponseEntity<ResponseModel<String>> setPin(@CurrentUser() CurrentUserDto currentUserDto, @Valid @RequestBody SetPinRequestDto setPinRequest) {
        return balanceService.setPin(currentUserDto.getUserId(), setPinRequest.getPin());
    }


}

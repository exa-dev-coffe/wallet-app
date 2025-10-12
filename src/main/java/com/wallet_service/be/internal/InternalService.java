package com.wallet_service.be.internal;

import com.wallet_service.be.balance.BalanceService;
import com.wallet_service.be.exception.BadRequestException;
import com.wallet_service.be.utils.commons.ResponseModel;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class InternalService {
    private BalanceService balanceService;

    public InternalService(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public ResponseEntity<ResponseModel<String>> pay(Integer userId, Double amount, String pin) {
        boolean success = balanceService.pay(userId, amount, pin);
        if (success) {
            ResponseModel<String> response = new ResponseModel<>(true, "Success Payment", null);
            return ResponseEntity.ok(response);
        } else {
            throw new BadRequestException("Balance is not enough");
        }
    }
}

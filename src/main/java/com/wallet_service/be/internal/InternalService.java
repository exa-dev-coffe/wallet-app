package com.wallet_service.be.internal;

import com.wallet_service.be.balance.BalanceService;
import com.wallet_service.be.exception.BadRequestException;
import com.wallet_service.be.internal.dto.PosQrisChargeRequestDto;
import com.wallet_service.be.internal.dto.PosQrisChargeResponseDto;
import com.wallet_service.be.internal.dto.PosWalletPayRequestDto;
import com.wallet_service.be.internal.dto.PosWalletPayResponseDto;
import com.wallet_service.be.utils.commons.ResponseModel;
import jakarta.transaction.Transactional;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class InternalService {
    private final BalanceService balanceService;

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

    @Transactional(Transactional.TxType.REQUIRED)
    public ResponseEntity<ResponseModel<PosWalletPayResponseDto>> payWithPosCode(PosWalletPayRequestDto requestDto) {
        PosWalletPayResponseDto res = balanceService.payWithPosCode(requestDto.getPaymentCode(), requestDto.getAmount(), requestDto.getOrderId());
        ResponseModel<PosWalletPayResponseDto> response = new ResponseModel<>(true, "POS Wallet payment successful", res);
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<ResponseModel<PosQrisChargeResponseDto>> chargePosQris(PosQrisChargeRequestDto requestDto) throws Exception {
        PosQrisChargeResponseDto res = balanceService.chargePosQris(requestDto);
        ResponseModel<PosQrisChargeResponseDto> response = new ResponseModel<>(true, "POS QRIS charge initiated successfully", res);
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<ResponseModel<Map<String, Object>>> checkPosQrisStatus(String orderId) throws Exception {
        JSONObject res = balanceService.checkPosQrisStatus(orderId);
        ResponseModel<Map<String, Object>> response = new ResponseModel<>(true, "POS QRIS status retrieved", res.toMap());
        return ResponseEntity.ok(response);
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public ResponseEntity<ResponseModel<String>> refundPosWallet(com.wallet_service.be.internal.dto.PosWalletRefundRequestDto requestDto) {
        boolean refunded = balanceService.refundPosWallet(requestDto.getUserId(), requestDto.getAmount(), requestDto.getOrderId());
        if (refunded) {
            return ResponseEntity.ok(new ResponseModel<>(true, "Wallet auto-refunded successfully", "OK"));
        } else {
            throw new BadRequestException("Failed to refund customer wallet");
        }
    }
}

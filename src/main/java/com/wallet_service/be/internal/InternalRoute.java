package com.wallet_service.be.internal;

import com.wallet_service.be.annotation.ValidateSignature;
import com.wallet_service.be.internal.dto.PaymentRequestDto;
import com.wallet_service.be.internal.dto.PosQrisChargeRequestDto;
import com.wallet_service.be.internal.dto.PosQrisChargeResponseDto;
import com.wallet_service.be.internal.dto.PosWalletPayRequestDto;
import com.wallet_service.be.internal.dto.PosWalletPayResponseDto;
import com.wallet_service.be.utils.commons.ResponseModel;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/internal")
public class InternalRoute {
    private final InternalService internalService;

    public InternalRoute(InternalService internalService) {
        this.internalService = internalService;
    }

    @PostMapping("/pay")
    @ValidateSignature
    public ResponseEntity<ResponseModel<String>> pay(@Valid @RequestBody PaymentRequestDto paymentRequestDto) {
        return internalService.pay(paymentRequestDto.getUserId(), paymentRequestDto.getAmount(), paymentRequestDto.getPin());
    }

    @PostMapping("/pos/wallet/pay")
    @ValidateSignature
    public ResponseEntity<ResponseModel<PosWalletPayResponseDto>> payWithPosCode(@Valid @RequestBody PosWalletPayRequestDto requestDto) {
        return internalService.payWithPosCode(requestDto);
    }

    @PostMapping("/pos/qris/charge")
    @ValidateSignature
    public ResponseEntity<ResponseModel<PosQrisChargeResponseDto>> chargePosQris(@Valid @RequestBody PosQrisChargeRequestDto requestDto) throws Exception {
        return internalService.chargePosQris(requestDto);
    }

    @GetMapping("/pos/qris/status/{orderId}")
    @ValidateSignature
    public ResponseEntity<ResponseModel<Map<String, Object>>> checkPosQrisStatus(@PathVariable("orderId") String orderId) throws Exception {
        return internalService.checkPosQrisStatus(orderId);
    }

    @PostMapping("/pos/wallet/refund")
    @ValidateSignature
    public ResponseEntity<ResponseModel<String>> refundPosWallet(@Valid @RequestBody com.wallet_service.be.internal.dto.PosWalletRefundRequestDto requestDto) {
        return internalService.refundPosWallet(requestDto);
    }
}

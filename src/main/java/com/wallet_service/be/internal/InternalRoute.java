package com.wallet_service.be.internal;

import com.wallet_service.be.annotation.ValidateSignature;
import com.wallet_service.be.internal.dto.PaymentRequestDto;
import com.wallet_service.be.utils.commons.ResponseModel;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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


}

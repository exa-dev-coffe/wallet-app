package com.time_tracker.be.lib;

import com.midtrans.service.MidtransSnapApi;
import com.time_tracker.be.balanceHistory.enums.StatusBalanceHistory;
import com.time_tracker.be.utils.commons.MidtransRequestDto;
import com.time_tracker.be.utils.commons.MidtransResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MidtransService {

    @Value("${midtrans.server-key}")
    private String serverKey;
    private final MidtransSnapApi snapApi;

    public MidtransService(MidtransSnapApi snapApi) { // ✅ Spring inject otomatis
        this.snapApi = snapApi;
    }

    public MidtransResponseDto createTransaction(MidtransRequestDto midtransRequestDto) throws Exception {
        JSONObject response = snapApi.createTransaction(midtransRequestDto.toMap());

        if (!response.has("redirect_url") || !response.has("token")) {
            throw new Exception("Failed to create Midtrans transaction");
        }

        return new MidtransResponseDto(
                response.getString("redirect_url"),
                response.getString("token")
        );
    }

    public boolean validateSignatureKey(String orderId, String statusCode, String grossAmount, String signatureKey) {
        String input = orderId + statusCode + grossAmount + serverKey;
        String generatedSignatureKey = org.apache.commons.codec.digest.DigestUtils.sha512Hex(input);
        return generatedSignatureKey.equals(signatureKey);
    }

    public StatusBalanceHistory mapTransactionStatus(String transactionStatus, String fraudStatus) {
        if ("capture".equals(transactionStatus)) {
            if ("challenge".equals(fraudStatus)) {
                return StatusBalanceHistory.PENDING;
            } else if ("accept".equals(fraudStatus)) {
                return StatusBalanceHistory.COMPLETED;
            }
        } else if ("settlement".equals(transactionStatus)) {
            return StatusBalanceHistory.COMPLETED;
        } else if ("deny".equals(transactionStatus) || "cancel".equals(transactionStatus) || "expire".equals(transactionStatus)) {
            return StatusBalanceHistory.FAILED;
        } else if ("pending".equals(transactionStatus)) {
            return StatusBalanceHistory.PENDING;
        }
        return StatusBalanceHistory.PENDING;
    }
}

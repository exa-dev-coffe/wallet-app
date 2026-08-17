package com.wallet_service.be.lib;

import com.midtrans.service.MidtransCoreApi;
import com.midtrans.service.MidtransSnapApi;
import com.wallet_service.be.balanceHistory.enums.StatusBalanceHistory;
import com.wallet_service.be.utils.commons.MidtransChargeRequestDto;
import com.wallet_service.be.utils.commons.MidtransChargeResponseDto;
import com.wallet_service.be.utils.commons.MidtransRequestDto;
import com.wallet_service.be.utils.commons.MidtransResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class MidtransService {

    @Value("${midtrans.server-key}")
    private String serverKey;
    private final MidtransCoreApi coreApi;
    private final MidtransSnapApi snapApi;

    public MidtransService(MidtransCoreApi coreApi, MidtransSnapApi snapApi) {
        this.coreApi = coreApi;
        this.snapApi = snapApi;
    }

    public MidtransChargeResponseDto chargeTransaction(MidtransChargeRequestDto chargeRequestDto) throws Exception {
        JSONObject response = coreApi.chargeTransaction(chargeRequestDto.toMap());
        log.info("Midtrans Core API response: {}", response);

        String statusCode = response.optString("status_code", "");
        if (statusCode.startsWith("4") || statusCode.startsWith("5")) {
            String statusMessage = response.optString("status_message", "Midtrans charge failed");
            throw new Exception("Failed to charge Midtrans transaction: " + statusMessage);
        }

        MidtransChargeResponseDto.MidtransChargeResponseDtoBuilder builder = MidtransChargeResponseDto.builder()
                .orderId(chargeRequestDto.getOrderId())
                .grossAmount(chargeRequestDto.getGrossAmount())
                .paymentType(response.optString("payment_type", chargeRequestDto.getPaymentType()))
                .transactionStatus(response.optString("transaction_status", "pending"))
                .transactionId(response.optString("transaction_id", null))
                .statusCode(statusCode)
                .statusMessage(response.optString("status_message", null))
                .expiryTime(response.optString("expiry_time", null));

        if (response.has("va_numbers")) {
            JSONArray vaArray = response.optJSONArray("va_numbers");
            if (vaArray != null && !vaArray.isEmpty()) {
                JSONObject firstVa = vaArray.getJSONObject(0);
                builder.bank(firstVa.optString("bank", chargeRequestDto.getBank()));
                builder.vaNumber(firstVa.optString("va_number", null));
            }
        } else if (response.has("permata_va_number")) {
            builder.bank("permata");
            builder.vaNumber(response.optString("permata_va_number", null));
        }

        if (response.has("bill_key") && response.has("biller_code")) {
            builder.bank("mandiri");
            builder.billKey(response.optString("bill_key", null));
            builder.billerCode(response.optString("biller_code", null));
        }

        if (response.has("qr_string")) {
            builder.qrString(response.optString("qr_string", null));
        }

        if (response.has("actions")) {
            JSONArray actions = response.optJSONArray("actions");
            if (actions != null) {
                for (int i = 0; i < actions.length(); i++) {
                    JSONObject action = actions.getJSONObject(i);
                    String actionName = action.optString("name", "");
                    String actionUrl = action.optString("url", "");
                    if ("generate-qr-code".equalsIgnoreCase(actionName)) {
                        builder.qrUrl(actionUrl);
                    } else if ("deeplink-redirect".equalsIgnoreCase(actionName)) {
                        builder.deeplinkUrl(actionUrl);
                    }
                }
            }
        }

        if (builder.build().getBank() == null && chargeRequestDto.getBank() != null && !chargeRequestDto.getBank().isBlank()) {
            builder.bank(chargeRequestDto.getBank().toLowerCase());
        }

        return builder.build();
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

    public boolean validateSignatureKey(UUID orderId, String statusCode, String grossAmount, String signatureKey) {
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


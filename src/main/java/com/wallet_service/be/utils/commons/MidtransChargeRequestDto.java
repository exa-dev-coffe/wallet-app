package com.wallet_service.be.utils.commons;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MidtransChargeRequestDto {
    private UUID orderId;
    private String customOrderId;
    private Double grossAmount;
    private String firstName;
    private String email;
    private String paymentType;
    private String bank;

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        String finalOrderId = (customOrderId != null && !customOrderId.isBlank())
                ? customOrderId
                : (orderId != null ? orderId.toString() : "");

        Map<String, Object> transactionDetails = new HashMap<>();
        transactionDetails.put("order_id", finalOrderId);
        transactionDetails.put("gross_amount", grossAmount.longValue());
        map.put("transaction_details", transactionDetails);

        Map<String, Object> customerDetails = new HashMap<>();
        customerDetails.put("first_name", firstName != null && !firstName.isBlank() ? firstName : "Customer");
        customerDetails.put("email", email != null && !email.isBlank() ? email : "customer@example.com");
        map.put("customer_details", customerDetails);

        String normalizedPaymentType = paymentType != null ? paymentType.trim().toLowerCase() : "qris";
        String normalizedBank = bank != null ? bank.trim().toLowerCase() : "";

        if ("bank_transfer".equalsIgnoreCase(normalizedPaymentType) || "va".equalsIgnoreCase(normalizedPaymentType)) {
            if ("mandiri".equalsIgnoreCase(normalizedBank) || "echannel".equalsIgnoreCase(normalizedBank)) {
                map.put("payment_type", "echannel");
                Map<String, Object> echannel = new HashMap<>();
                echannel.put("bill_info1", "Payment For:");
                echannel.put("bill_info2", "Wallet Top Up");
                map.put("echannel", echannel);
            } else if ("permata".equalsIgnoreCase(normalizedBank)) {
                map.put("payment_type", "permata");
            } else {
                map.put("payment_type", "bank_transfer");
                Map<String, Object> bankTransfer = new HashMap<>();
                bankTransfer.put("bank", normalizedBank.isEmpty() ? "bca" : normalizedBank);
                map.put("bank_transfer", bankTransfer);
            }
        } else if ("echannel".equalsIgnoreCase(normalizedPaymentType) || "mandiri".equalsIgnoreCase(normalizedPaymentType)) {
            map.put("payment_type", "echannel");
            Map<String, Object> echannel = new HashMap<>();
            echannel.put("bill_info1", "Payment For:");
            echannel.put("bill_info2", "Wallet Top Up");
            map.put("echannel", echannel);
        } else if ("gopay".equalsIgnoreCase(normalizedPaymentType)) {
            map.put("payment_type", "gopay");
            Map<String, Object> gopay = new HashMap<>();
            gopay.put("enable_callback", true);
            map.put("gopay", gopay);
        } else if ("shopeepay".equalsIgnoreCase(normalizedPaymentType)) {
            map.put("payment_type", "shopeepay");
            Map<String, Object> shopeepay = new HashMap<>();
            shopeepay.put("callback_url", "https://example.com");
            map.put("shopeepay", shopeepay);
        } else {
            map.put("payment_type", "qris");
            Map<String, Object> qris = new HashMap<>();
            qris.put("acquirer", "gopay");
            map.put("qris", qris);
        }

        return map;
    }
}

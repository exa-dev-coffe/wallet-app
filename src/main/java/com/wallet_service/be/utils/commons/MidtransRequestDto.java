package com.wallet_service.be.utils.commons;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
public class MidtransRequestDto {

    @JsonProperty("transaction_details")
    private TransactionDetails transactionDetails;

    @JsonProperty("customer_details")
    private CustomerDetails customerDetails;

    public MidtransRequestDto(UUID orderId, Double grossAmount, String firstName, String email) {
        this.transactionDetails = new TransactionDetails(orderId, grossAmount);
        this.customerDetails = new CustomerDetails(firstName, email);
    }

    @Data
    public static class TransactionDetails {
        @JsonProperty("order_id")
        private UUID orderId;
        @JsonProperty("gross_amount")
        private Double grossAmount;

        public TransactionDetails(UUID orderId, Double grossAmount) {
            this.orderId = orderId;
            this.grossAmount = grossAmount;
        }
    }

    @Data
    public static class CustomerDetails {
        @JsonProperty("first_name")
        private String firstName;
        private String email;

        public CustomerDetails(String firstName, String email) {
            this.firstName = firstName;
            this.email = email;
        }
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("transaction_details", Map.of(
                "order_id", transactionDetails.getOrderId(),
                "gross_amount", transactionDetails.getGrossAmount()
        ));
        map.put("customer_details", Map.of(
                "first_name", customerDetails.getFirstName(),
                "email", customerDetails.getEmail()
        ));
        return map;
    }
}

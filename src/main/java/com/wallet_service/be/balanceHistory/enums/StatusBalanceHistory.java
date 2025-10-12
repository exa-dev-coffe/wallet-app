package com.wallet_service.be.balanceHistory.enums;

import lombok.Getter;

@Getter
public enum StatusBalanceHistory {
    PENDING("pending"),
    COMPLETED("completed"),
    CANCELLED("cancelled"),
    FAILED("failed");

    private final String value;

    StatusBalanceHistory(String value) {
        this.value = value;
    }

    public static StatusBalanceHistory fromValue(String value) {
        for (StatusBalanceHistory status : StatusBalanceHistory.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown balance history status: " + value);
    }
}

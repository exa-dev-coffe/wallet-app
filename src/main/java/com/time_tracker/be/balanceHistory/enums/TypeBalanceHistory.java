package com.time_tracker.be.balanceHistory.enums;

import lombok.Getter;

@Getter
public enum TypeBalanceHistory {
    PAYMENT("payment"),
    TOPUP("topup");

    private final String value;

    TypeBalanceHistory(String value) {
        this.value = value;
    }

    public static TypeBalanceHistory fromValue(String value) {
        for (TypeBalanceHistory type : TypeBalanceHistory.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown balance history type: " + value);
    }
}

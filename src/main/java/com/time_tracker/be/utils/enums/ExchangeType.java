package com.time_tracker.be.utils.enums;

public enum ExchangeType {
    DIRECT("direct"),
    FANOUT("fanout"),
    TOPIC("topic"),
    HEADERS("headers");

    private final String value;

    ExchangeType(String type) {
        this.value = type;
    }

    public String getValue() {
        return value;
    }

    public static ExchangeType fromValue(String value) {
        for (ExchangeType type : ExchangeType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown exchange type: " + value);
    }
}

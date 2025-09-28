package com.time_tracker.be.resolver;

import java.util.HashMap;
import java.util.Map;

public class ConstraintMessageResolver {
    private static final Map<String, String> constraintMessages = new HashMap<>();

    static {
        // mapping constraint → pesan
        constraintMessages.put("tm_accounts_email_key", "Email already exists");
        // tambahin constraint lain di sini
    }

    public static String resolveMessage(String dbMessage) {
        if (dbMessage == null) return "Data integrity violation";

        for (Map.Entry<String, String> entry : constraintMessages.entrySet()) {
            if (dbMessage.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return "Data integrity violation";
    }
}
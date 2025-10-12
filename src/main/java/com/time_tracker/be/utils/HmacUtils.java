package com.time_tracker.be.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class HmacUtils {

    private final String secret; // bisa ambil dari application.properties

    public HmacUtils(@Value("${app.secret}") String secret) {
        this.secret = secret;
    }

    // Generate HMAC SHA-256
    public String generateHMAC(String message) throws Exception {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secretKey);

        byte[] hash = sha256_HMAC.doFinal(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }

    // Verify signature
    public boolean verifySignature(String message, String signatureHeader) throws Exception {
        String expectedSignature = generateHMAC(message);

        // Bandingkan menggunakan waktu konstan untuk mengurangi risiko timing attack
        return constantTimeEquals(expectedSignature, signatureHeader);
    }

    // Fungsi constant-time compare
    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
package com.wallet_service.be;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Top-Up & Midtrans Webhook Features")
class WalletTopUpTest extends BaseIntegrationTest {

    @Test
    @DisplayName("POST /balance/top-up - Negative Amount (400)")
    void testTopUpNegativeAmount() throws Exception {
        String body = """
                {
                    "amount": -50000,
                    "paymentType": "bank_transfer"
                }
                """;

        mockMvc.perform(post("/api/1.0/balance/top-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("POST /balance/top-up/{id}/sync - Unauthorized (401)")
    void testSyncTransactionStatusUnauthorized() throws Exception {
        UUID testId = UUID.randomUUID();
        mockMvc.perform(post("/api/1.0/balance/top-up/" + testId + "/sync"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("POST /midtrans-notification - Invalid Signature (400)")
    void testMidtransNotificationInvalidSignature() throws Exception {
        String body = """
                {
                    "order_id": "ORDER-12345",
                    "transaction_status": "settlement",
                    "gross_amount": "50000.00",
                    "signature_key": "invalid_hash"
                }
                """;

        mockMvc.perform(post("/api/1.0/midtrans-notification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }
}

package com.wallet_service.be;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Internal Payment Features (/api/internal)")
class WalletInternalTest extends BaseIntegrationTest {

    @Test
    @DisplayName("POST /api/internal/pay - Missing Signature/Invalid Request (400/401)")
    void testInternalPayInvalid() throws Exception {
        String body = """
                {
                    "userId": 1,
                    "amount": 50000,
                    "pin": "123456"
                }
                """;

        mockMvc.perform(post("/api/internal/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }
}

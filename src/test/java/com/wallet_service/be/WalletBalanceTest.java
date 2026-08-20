package com.wallet_service.be;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Balance & PIN Features (/api/1.0/balance)")
class WalletBalanceTest extends BaseIntegrationTest {

    @Test
    @DisplayName("GET /balance - Unauthorized (401)")
    void testGetBalanceUnauthorized() throws Exception {
        mockMvc.perform(get("/api/1.0/balance"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("POST /balance/activate - Unauthorized (401)")
    void testActivatePinUnauthorized() throws Exception {
        String body = """
                {
                    "pin": "123456"
                }
                """;

        mockMvc.perform(post("/api/1.0/balance/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("POST /balance/activate - Invalid PIN Format (400)")
    void testActivatePinInvalidFormat() throws Exception {
        String body = """
                {
                    "pin": "12"
                }
                """;

        mockMvc.perform(post("/api/1.0/balance/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }
}

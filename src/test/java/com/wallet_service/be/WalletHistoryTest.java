package com.wallet_service.be;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Balance History Features (/api/1.0/balance-history)")
class WalletHistoryTest extends BaseIntegrationTest {

    @Test
    @DisplayName("GET /balance-history - Unauthorized (401)")
    void testGetAllBalanceHistoryUnauthorized() throws Exception {
        mockMvc.perform(get("/api/1.0/balance-history"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("GET /balance-history/{id} - Unauthorized (401)")
    void testGetBalanceHistoryDetailUnauthorized() throws Exception {
        UUID testId = UUID.randomUUID();
        mockMvc.perform(get("/api/1.0/balance-history/" + testId))
                .andExpect(status().is4xxClientError());
    }
}

package com.wallet_service.be;

import com.wallet_service.be.balance.BalanceModel;
import com.wallet_service.be.balance.BalanceRepository;
import com.wallet_service.be.utils.PasswordUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Internal Payment Features (/api/internal + Direct DB Assertion)")
class WalletInternalTest extends BaseIntegrationTest {

    @Autowired
    private BalanceRepository balanceRepository;

    private final int testUserId = 100;

    @BeforeEach
    void setUpUserBalance() {
        BalanceModel balance = balanceRepository.findByUserId(testUserId);
        if (balance == null) {
            balance = new BalanceModel();
            balance.setUserId(testUserId);
            balance.setBalance(150000.0);
            balance.setActive(true);
        }
        balance.setPin(PasswordUtils.hashPassword("123456"));
        balance.setBalance(150000.0);
        balanceRepository.save(balance);
    }

    @Test
    @DisplayName("POST /api/internal/pay - Real Internal Payment Success (200 + Direct DB Assertion)")
    void testInternalPaySuccessRealDB() throws Exception {
        String body = """
                {
                    "userId": 100,
                    "amount": 25000,
                    "pin": "123456"
                }
                """;

        String timestamp = Instant.now().toString();
        String signature = createHmacSignature("", timestamp, body);

        mockMvc.perform(post("/api/internal/pay")
                        .header("X-Signature", signature)
                        .header("X-Timestamp", timestamp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // VERIFY REAL DATABASE STATE
        BalanceModel updatedBalance = balanceRepository.findByUserId(testUserId);
        assertNotNull(updatedBalance);
        assertEquals(125000.0, updatedBalance.getBalance(), "User balance in PostgreSQL DB must be reduced by 25000");
    }

    @Test
    @DisplayName("POST /api/internal/pay - Missing Signature or Timestamp (401)")
    void testInternalPayMissingHeaders() throws Exception {
        String body = """
                {
                    "userId": 100,
                    "amount": 50000,
                    "pin": "123456"
                }
                """;

        mockMvc.perform(post("/api/internal/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("POST /api/internal/pay - Invalid Signature (401)")
    void testInternalPayInvalidSignature() throws Exception {
        String body = """
                {
                    "userId": 100,
                    "amount": 50000,
                    "pin": "123456"
                }
                """;

        String timestamp = Instant.now().toString();

        mockMvc.perform(post("/api/internal/pay")
                        .header("X-Signature", "invalid-signature")
                        .header("X-Timestamp", timestamp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }
}

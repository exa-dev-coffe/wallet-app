package com.wallet_service.be;

import com.wallet_service.be.balance.BalanceModel;
import com.wallet_service.be.balance.BalanceRepository;
import com.wallet_service.be.balanceHistory.BalancehistoryRepository;
import com.wallet_service.be.lib.MidtransService;
import com.wallet_service.be.lib.RabbitmqService;
import com.wallet_service.be.utils.PasswordUtils;
import com.wallet_service.be.utils.commons.MidtransChargeRequestDto;
import com.wallet_service.be.utils.commons.MidtransChargeResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Real DB Integration Tests - Top-Up & Midtrans Features (/api/1.0/balance)")
class WalletTopUpTest extends BaseIntegrationTest {

    @Autowired
    private BalanceRepository balanceRepository;

    @Autowired
    private BalancehistoryRepository balancehistoryRepository;

    private final int testUserId = 100;
    private final String testEmail = "user@test.com";

    @BeforeEach
    void setUpRealDatabase() {
        BalanceModel balance = balanceRepository.findByUserId(testUserId);
        if (balance == null) {
            balance = new BalanceModel();
            balance.setUserId(testUserId);
            balance.setBalance(150000.0);
            balance.setActive(true);
            balance.setPin(PasswordUtils.hashPassword("123456"));
            balanceRepository.save(balance);
        }
    }

    @Test
    @DisplayName("POST /balance/top-up - Success HTTP 200 OK & PostgreSQL Persistence")
    void testTopUpSuccessRealDB() throws Exception {
        String token = generateTestToken(testUserId, testEmail, "customer");
        String body = """
                {
                    "amount": 50000,
                    "paymentType": "bank_transfer",
                    "bank": "bca"
                }
                """;

        MidtransChargeResponseDto mockChargeRes = MidtransChargeResponseDto.builder()
                .paymentType("bank_transfer")
                .bank("bca")
                .vaNumber("1234567890")
                .transactionStatus("pending")
                .build();
        when(midtransService.chargeTransaction(any(MidtransChargeRequestDto.class))).thenReturn(mockChargeRes);

        mockMvc.perform(post("/api/1.0/balance/top-up")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.vaNumber").value("1234567890"))
                .andExpect(jsonPath("$.data.bank").value("bca"));

        // Verify transaction record was created in PostgreSQL database
        assertTrue(balancehistoryRepository.count() >= 1, "A top-up transaction record must be created in PostgreSQL DB");
    }

    @Test
    @DisplayName("POST /balance/top-up - Negative Amount (400)")
    void testTopUpNegativeAmount() throws Exception {
        String token = generateTestToken(testUserId, testEmail, "customer");
        String body = """
                {
                    "amount": -50000,
                    "paymentType": "bank_transfer"
                }
                """;

        mockMvc.perform(post("/api/1.0/balance/top-up")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}

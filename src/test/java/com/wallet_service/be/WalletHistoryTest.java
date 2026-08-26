package com.wallet_service.be;

import com.wallet_service.be.balance.BalanceModel;
import com.wallet_service.be.balance.BalanceRepository;
import com.wallet_service.be.balanceHistory.BalancehistoryModel;
import com.wallet_service.be.balanceHistory.BalancehistoryRepository;
import com.wallet_service.be.balanceHistory.enums.StatusBalanceHistory;
import com.wallet_service.be.balanceHistory.enums.TypeBalanceHistory;
import com.wallet_service.be.utils.PasswordUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Real DB Integration Tests - Balance History (/api/1.0/balance-history)")
class WalletHistoryTest extends BaseIntegrationTest {

    @Autowired
    private BalanceRepository balanceRepository;

    @Autowired
    private BalancehistoryRepository balancehistoryRepository;

    private final int testUserId = 100;
    private final String testEmail = "user@test.com";
    private BalancehistoryModel mockHistoryRecord;

    @BeforeEach
    void setUpRealDatabase() {
        BalanceModel balance = balanceRepository.findByUserId(testUserId);
        if (balance == null) {
            balance = new BalanceModel();
            balance.setUserId(testUserId);
            balance.setBalance(150000.0);
            balance.setActive(true);
            balance.setPin(PasswordUtils.hashPassword("123456"));
            balance = balanceRepository.save(balance);
        }

        mockHistoryRecord = new BalancehistoryModel();
        mockHistoryRecord.setBalance(balance);
        mockHistoryRecord.setAmount(50000.0);
        mockHistoryRecord.setType(TypeBalanceHistory.TOPUP);
        mockHistoryRecord.setStatus(StatusBalanceHistory.COMPLETED);
        mockHistoryRecord.setUserEmail(testEmail);
        mockHistoryRecord.setUserName("Test User");
        mockHistoryRecord = balancehistoryRepository.save(mockHistoryRecord);
    }

    @Test
    @DisplayName("GET /balance-history - Real PostgreSQL Query Success HTTP 200 OK")
    void testGetBalanceHistoryRealDB() throws Exception {
        String token = generateTestToken(testUserId, testEmail, "customer");

        mockMvc.perform(get("/api/1.0/balance-history")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.data").isArray());
    }

    @Test
    @DisplayName("GET /balance-history/{id} - Real PostgreSQL Detail Query Success HTTP 200 OK")
    void testGetBalanceHistoryDetailRealDB() throws Exception {
        String token = generateTestToken(testUserId, testEmail, "customer");

        mockMvc.perform(get("/api/1.0/balance-history/" + mockHistoryRecord.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.amount").value(50000.0))
                .andExpect(jsonPath("$.data.type").value("topup"));
    }
}

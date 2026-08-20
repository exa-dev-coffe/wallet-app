package com.wallet_service.be;

import com.wallet_service.be.balance.BalanceModel;
import com.wallet_service.be.balance.BalanceRepository;
import com.wallet_service.be.lib.RabbitmqService;
import com.wallet_service.be.utils.PasswordUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Real DB & Redis Integration Tests - Balance & PIN Features (/api/1.0/balance)")
class WalletBalanceTest extends BaseIntegrationTest {

    @Autowired
    private BalanceRepository balanceRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private final String testEmail = "user@test.com";
    private final int testUserId = 100;

    @BeforeEach
    void setUpDatabaseAndRedis() {
        // Clear Redis Keys for test user
        redisTemplate.delete("wallet:resetPin:sendCount:" + testEmail);
        redisTemplate.delete("wallet:resetPin:code:" + testEmail);

        // Ensure user balance exists in REAL Postgres Test Database
        BalanceModel balance = balanceRepository.findByUserId(testUserId);
        if (balance == null) {
            balance = new BalanceModel();
            balance.setUserId(testUserId);
            balance.setBalance(150000.0);
            balance.setActive(true);
        }
        balance.setPin(PasswordUtils.hashPassword("123456"));
        balanceRepository.save(balance);
    }

    @Test
    @DisplayName("POST /balance/change-pin - REAL DB Update & Password Verification")
    void testRealChangePinIntegration() throws Exception {
        String token = generateTestToken(testUserId, testEmail, "customer");
        String requestBody = """
                {
                    "oldPin": "123456",
                    "newPin": "654321"
                }
                """;

        // Perform HTTP POST request through real controllers, filters, and services
        mockMvc.perform(post("/api/1.0/balance/change-pin")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("PIN berhasil diubah"));

        // VERIFY REAL DATABASE UPDATE: Query PostgreSQL DB directly!
        BalanceModel updatedBalance = balanceRepository.findByUserId(testUserId);
        assertNotNull(updatedBalance);
        assertTrue(PasswordUtils.matches("654321", updatedBalance.getPin()), "PIN in PostgreSQL DB should be updated to new hashed PIN");
    }

    @Test
    @DisplayName("POST /balance/reset-pin/send-code & /reset-pin - REAL Redis OTP & DB Update")
    void testRealResetPinSendCodeAndResetIntegration() throws Exception {
        String token = generateTestToken(testUserId, testEmail, "customer");

        // STEP 1: Send Reset PIN Code via HTTP
        mockMvc.perform(post("/api/1.0/balance/reset-pin/send-code")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Kode verifikasi reset PIN telah dikirim ke email Anda."));

        // STEP 2: READ REAL OTP CODE DIRECTLY FROM REDIS!
        String redisCodeKey = "wallet:resetPin:code:" + testEmail;
        String realOtpCodeFromRedis = (String) redisTemplate.opsForValue().get(redisCodeKey);

        assertNotNull(realOtpCodeFromRedis, "OTP code must be generated and stored in Redis!");
        assertEquals(6, realOtpCodeFromRedis.length(), "OTP code from Redis must be 6 digits");

        // STEP 3: Submit Reset PIN HTTP Request using REAL OTP code extracted from Redis!
        String resetRequestBody = String.format("""
                {
                    "code": "%s",
                    "newPin": "999888"
                }
                """, realOtpCodeFromRedis);

        mockMvc.perform(post("/api/1.0/balance/reset-pin")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetRequestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("PIN wallet berhasil diperbarui"));

        // STEP 4: VERIFY REAL DATABASE & REDIS STATE
        BalanceModel updatedBalance = balanceRepository.findByUserId(testUserId);
        assertNotNull(updatedBalance);
        assertTrue(PasswordUtils.matches("999888", updatedBalance.getPin()), "PIN in DB should be updated to 999888");

        // Verify OTP code was deleted from Redis after successful reset
        Object deletedRedisCode = redisTemplate.opsForValue().get(redisCodeKey);
        assertNull(deletedRedisCode, "OTP code should be deleted from Redis after usage");
    }

    @Test
    @DisplayName("POST /balance/reset-pin/send-code - REAL Redis Rate Limit 3x/day (429)")
    void testRealResetPinRateLimitExceededIntegration() throws Exception {
        String token = generateTestToken(testUserId, testEmail, "customer");

        // Simulate user already sending OTP code 3 times in Redis
        String countKey = "wallet:resetPin:sendCount:" + testEmail;
        redisTemplate.opsForValue().set(countKey, 3);

        // Perform HTTP request - Expect 429 Too Many Requests from real service
        mockMvc.perform(post("/api/1.0/balance/reset-pin/send-code")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Batas maksimum pengiriman kode verifikasi PIN (3 kali) hari ini telah tercapai."));
    }
}

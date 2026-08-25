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

    @Test
    @DisplayName("POS Flow - Generate POS Code and Pay with Code (200 + Direct DB & Redis Assertion)")
    void testPosCodeGenerationAndPayment() throws Exception {
        // 1. Generate POS Code as user
        String token = generateTestToken(testUserId, "customer@test.com", "customer");
        String genBody = """
                {
                    "pin": "123456"
                }
                """;

        String genResp = mockMvc.perform(post("/api/1.0/balance/generate-pos-code")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(genBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentCode").isString())
                .andReturn().getResponse().getContentAsString();

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var jsonNode = mapper.readTree(genResp);
        String paymentCode = jsonNode.get("data").get("paymentCode").asText();

        // 2. Pay using POS Code via internal endpoint
        String payBody = String.format("""
                {
                    "paymentCode": "%s",
                    "amount": 50000.0,
                    "orderId": 999
                }
                """, paymentCode);

        String timestamp = Instant.now().toString();
        String signature = createHmacSignature("", timestamp, payBody);

        mockMvc.perform(post("/api/internal/pos/wallet/pay")
                        .header("X-Signature", signature)
                        .header("X-Timestamp", timestamp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(testUserId))
                .andExpect(jsonPath("$.data.amountPaid").value(50000.0))
                .andExpect(jsonPath("$.data.remainingBalance").value(100000.0));

        // 3. Verify Database balance is reduced
        BalanceModel updated = balanceRepository.findByUserId(testUserId);
        assertNotNull(updated);
        assertEquals(100000.0, updated.getBalance());

        // 4. Verify Payment Code is burned/deleted (Replay attempt must fail)
        mockMvc.perform(post("/api/internal/pos/wallet/pay")
                        .header("X-Signature", signature)
                        .header("X-Timestamp", timestamp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Concurrency Race Condition - Multiple Simultaneous Payments (Pessimistic Locking / FOR UPDATE)")
    void testConcurrentPaymentRaceCondition() throws Exception {
        // Set initial balance = 100,000
        BalanceModel balance = balanceRepository.findByUserId(testUserId);
        balance.setBalance(100000.0);
        balanceRepository.save(balance);

        int numberOfConcurrentRequests = 5;
        double amountPerRequest = 30000.0;
        // Total attempt = 5 * 30,000 = 150,000
        // Available = 100,000
        // Expected: Exactly 3 succeed (90,000 deducted), exactly 2 fail (insufficient balance), final balance = 10,000

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(numberOfConcurrentRequests);
        java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch doneLatch = new java.util.concurrent.CountDownLatch(numberOfConcurrentRequests);

        java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger failCount = new java.util.concurrent.atomic.AtomicInteger(0);

        String body = String.format("""
                {
                    "userId": %d,
                    "amount": %.1f,
                    "pin": "123456"
                }
                """, testUserId, amountPerRequest);

        for (int i = 0; i < numberOfConcurrentRequests; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // wait for gun shot to start all threads at the exact same millisecond
                    String timestamp = Instant.now().toString();
                    String signature = createHmacSignature("", timestamp, body);

                    var result = mockMvc.perform(post("/api/internal/pay")
                                    .header("X-Signature", signature)
                                    .header("X-Timestamp", timestamp)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body))
                            .andReturn();

                    if (result.getResponse().getStatus() == 200) {
                        successCount.incrementAndGet();
                    } else {
                        failCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Fire all threads simultaneously
        startLatch.countDown();
        doneLatch.await(10, java.util.concurrent.TimeUnit.SECONDS);
        executor.shutdown();

        // ASSERTIONS
        assertEquals(3, successCount.get(), "Exactly 3 payments of 30,000 must succeed from 100,000 balance");
        assertEquals(2, failCount.get(), "Exactly 2 payments must fail due to insufficient balance");

        // Verify Database state
        BalanceModel finalBalance = balanceRepository.findByUserId(testUserId);
        assertNotNull(finalBalance);
        assertEquals(10000.0, finalBalance.getBalance(), "Final DB balance must be exactly 10,000.0 (never negative or lost update)");
    }
}

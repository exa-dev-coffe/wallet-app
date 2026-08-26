package com.wallet_service.be;

import com.sun.net.httpserver.HttpServer;
import com.wallet_service.be.admin.AdminWalletService;
import com.wallet_service.be.balance.BalanceModel;
import com.wallet_service.be.balance.BalanceRepository;
import com.wallet_service.be.utils.PasswordUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Real Integration Tests - Admin Wallet & Email PIN Reset (/api/1.0/admin/wallets)")
class AdminWalletIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private BalanceRepository balanceRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private AdminWalletService adminWalletService;

    private static HttpServer mockAccountServer;
    private static int mockPort;

    private final String adminEmail = "admin@test.com";
    private final int adminUserId = 1;

    private final String customerEmail = "customer.admin.test@test.com";
    private final int customerUserId = 200;

    @BeforeAll
    static void startMockAccountService() throws Exception {
        mockAccountServer = HttpServer.create(new InetSocketAddress(0), 0);
        mockPort = mockAccountServer.getAddress().getPort();
        mockAccountServer.createContext("/api/internal/user-by-email", exchange -> {
            String uriStr = exchange.getRequestURI().toString();
            String response;
            int statusCode;
            if (uriStr != null && uriStr.contains("customer.admin.test")) {
                statusCode = 200;
                response = "{\"success\":true,\"message\":\"Found\",\"data\":{\"userId\":200,\"email\":\"customer.admin.test@test.com\"}}";
            } else {
                statusCode = 404;
                response = "{\"success\":false,\"message\":\"User not found\"}";
            }
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        });
        mockAccountServer.setExecutor(null);
        mockAccountServer.start();
    }

    @AfterAll
    static void stopMockAccountService() {
        if (mockAccountServer != null) {
            mockAccountServer.stop(0);
        }
    }

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adminWalletService, "accountServiceUrl", "http://localhost:" + mockPort);

        redisTemplate.delete("wallet:resetPin:sendCount:" + customerEmail);
        redisTemplate.delete("wallet:resetPin:code:" + customerEmail);

        BalanceModel customerBalance = balanceRepository.findByUserId(customerUserId);
        if (customerBalance == null) {
            customerBalance = new BalanceModel();
            customerBalance.setUserId(customerUserId);
            customerBalance.setBalance(250000.0);
            customerBalance.setActive(true);
        }
        customerBalance.setPin(PasswordUtils.hashPassword("111111"));
        balanceRepository.save(customerBalance);
    }

    @Test
    @DisplayName("GET /admin/wallets & /admin/wallets/summary - Admin gets customer wallets and summary stats")
    void testGetAdminWalletsSuccess() throws Exception {
        String adminToken = generateTestToken(adminUserId, adminEmail, "admin");

        mockMvc.perform(get("/api/1.0/admin/wallets")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.data").isArray());

        mockMvc.perform(get("/api/1.0/admin/wallets/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalActiveWallets").exists());
    }

    @Test
    @DisplayName("POST /admin/wallets/reset-pin/send-code & /reset-pin - Full Admin Email Verification PIN Reset Flow")
    void testAdminResetPinFlowSuccess() throws Exception {
        String adminToken = generateTestToken(adminUserId, adminEmail, "admin");

        // 1. Admin triggers sending code to customer email
        String sendCodeBody = String.format("""
                {
                    "email": "%s"
                }
                """, customerEmail);

        mockMvc.perform(post("/api/1.0/admin/wallets/reset-pin/send-code")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sendCodeBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 2. Read real OTP code from Redis
        String codeKey = "wallet:resetPin:code:" + customerEmail;
        String otpCode = (String) redisTemplate.opsForValue().get(codeKey);
        assertNotNull(otpCode, "OTP code must exist in Redis after Admin trigger!");
        assertEquals(6, otpCode.length());

        // 3. Admin submits reset PIN request using code from customer email
        String resetBody = String.format("""
                {
                    "userId": %d,
                    "email": "%s",
                    "code": "%s",
                    "newPin": "654321"
                }
                """, customerUserId, customerEmail, otpCode);

        mockMvc.perform(post("/api/1.0/admin/wallets/reset-pin")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 4. Verify Database update
        BalanceModel updatedBalance = balanceRepository.findByUserId(customerUserId);
        assertNotNull(updatedBalance);
        assertTrue(PasswordUtils.matches("654321", updatedBalance.getPin()), "Customer PIN should be updated to 654321");
    }

    @Test
    @DisplayName("POST /admin/wallets/{userId}/toggle-status - Toggle active/suspended wallet status")
    void testToggleWalletStatusSuccess() throws Exception {
        String adminToken = generateTestToken(adminUserId, adminEmail, "admin");

        // Suspend wallet
        mockMvc.perform(post("/api/1.0/admin/wallets/" + customerUserId + "/toggle-status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        BalanceModel suspendedBalance = balanceRepository.findByUserId(customerUserId);
        assertFalse(suspendedBalance.isActive(), "Wallet should be suspended (isActive = false)");

        // Activate wallet again
        mockMvc.perform(post("/api/1.0/admin/wallets/" + customerUserId + "/toggle-status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        BalanceModel activatedBalance = balanceRepository.findByUserId(customerUserId);
        assertTrue(activatedBalance.isActive(), "Wallet should be active (isActive = true)");
    }
}

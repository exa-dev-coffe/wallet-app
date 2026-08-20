package com.wallet_service.be.balance;

import com.wallet_service.be.exception.BadRequestException;
import com.wallet_service.be.exception.NotFoundException;
import com.wallet_service.be.exception.TooManyRequestException;
import com.wallet_service.be.lib.MidtransService;
import com.wallet_service.be.lib.RabbitmqService;
import com.wallet_service.be.balanceHistory.BalancehistoryService;
import com.wallet_service.be.utils.PasswordUtils;
import com.wallet_service.be.utils.commons.ResponseModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BalanceService Unit Tests (Success & Exception Flows)")
class BalanceServiceTest {

    @Mock
    private BalanceRepository balanceRepository;

    @Mock
    private MidtransService midtransService;

    @Mock
    private BalancehistoryService balancehistoryService;

    @Mock
    private RabbitmqService rabbitmqService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private BalanceService balanceService;

    private BalanceModel mockActiveBalance;

    @BeforeEach
    void setUp() {
        mockActiveBalance = new BalanceModel();
        mockActiveBalance.setId(1);
        mockActiveBalance.setUserId(100);
        mockActiveBalance.setActive(true);
        mockActiveBalance.setPin(PasswordUtils.hashPassword("123456"));
        mockActiveBalance.setBalance(50000.0);
    }

    // ==========================================
    // 1. CHANGE PIN TESTS
    // ==========================================

    @Test
    @DisplayName("changePin() - Success")
    void testChangePinSuccess() {
        when(balanceRepository.findByUserId(100)).thenReturn(mockActiveBalance);

        ResponseEntity<ResponseModel<String>> response = balanceService.changePin(100, "123456", "654321");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("PIN berhasil diubah", response.getBody().getMessage());

        verify(balanceRepository, times(1)).save(mockActiveBalance);
    }

    @Test
    @DisplayName("changePin() - Failed when Old PIN is incorrect")
    void testChangePinWrongOldPin() {
        when(balanceRepository.findByUserId(100)).thenReturn(mockActiveBalance);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            balanceService.changePin(100, "999999", "654321");
        });

        assertEquals("PIN saat ini salah", exception.getMessage());
        verify(balanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("changePin() - Failed when Wallet not activated")
    void testChangePinWalletNotActive() {
        when(balanceRepository.findByUserId(100)).thenReturn(null);

        NotFoundException exception = assertThrows(NotFoundException.class, () -> {
            balanceService.changePin(100, "123456", "654321");
        });

        assertEquals("Wallet belum diaktifkan", exception.getMessage());
    }

    // ==========================================
    // 2. SEND RESET PIN CODE (OTP) TESTS
    // ==========================================

    @Test
    @DisplayName("sendResetPinCode() - Success")
    void testSendResetPinCodeSuccess() throws Exception {
        when(balanceRepository.findByUserId(100)).thenReturn(mockActiveBalance);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("wallet:resetPin:sendCount:user@test.com")).thenReturn(null);

        ResponseEntity<ResponseModel<String>> response = balanceService.sendResetPinCode(100, "user@test.com");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("Kode verifikasi reset PIN telah dikirim ke email Anda.", response.getBody().getMessage());

        // Verify count set & code set in Redis
        verify(valueOperations, times(1)).set(eq("wallet:resetPin:sendCount:user@test.com"), eq(1), any(java.time.Duration.class));
        verify(valueOperations, times(1)).set(eq("wallet:resetPin:code:user@test.com"), anyString(), any(java.time.Duration.class));

        // Verify RabbitMQ message sent
        verify(rabbitmqService, times(1)).sendMessage(
                eq("Email Reset PIN Code"),
                eq("emailQueue.resetPin"),
                eq("email.queue"),
                any(),
                any(),
                anyString(),
                anyBoolean(),
                anyBoolean(),
                anyBoolean(),
                any()
        );
    }

    @Test
    @DisplayName("sendResetPinCode() - Failed when daily limit (3x/day) reached")
    void testSendResetPinCodeRateLimitExceeded() {
        when(balanceRepository.findByUserId(100)).thenReturn(mockActiveBalance);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("wallet:resetPin:sendCount:user@test.com")).thenReturn(3);

        TooManyRequestException exception = assertThrows(TooManyRequestException.class, () -> {
            balanceService.sendResetPinCode(100, "user@test.com");
        });

        assertEquals("Batas maksimum pengiriman kode verifikasi PIN (3 kali) hari ini telah tercapai.", exception.getMessage());
    }

    // ==========================================
    // 3. RESET PIN CONFIRMATION TESTS
    // ==========================================

    @Test
    @DisplayName("resetPin() - Success")
    void testResetPinSuccess() {
        when(balanceRepository.findByUserId(100)).thenReturn(mockActiveBalance);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("wallet:resetPin:code:user@test.com")).thenReturn("123456");

        ResponseEntity<ResponseModel<String>> response = balanceService.resetPin(100, "user@test.com", "123456", "654321");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals("PIN wallet berhasil diperbarui", response.getBody().getMessage());

        // Verify balance updated & code deleted from Redis
        verify(balanceRepository, times(1)).save(mockActiveBalance);
        verify(redisTemplate, times(1)).delete("wallet:resetPin:code:user@test.com");
    }

    @Test
    @DisplayName("resetPin() - Failed when OTP code is wrong or expired")
    void testResetPinInvalidCode() {
        when(balanceRepository.findByUserId(100)).thenReturn(mockActiveBalance);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("wallet:resetPin:code:user@test.com")).thenReturn("123456");

        BadRequestException exception = assertThrows(BadRequestException.class, () -> {
            balanceService.resetPin(100, "user@test.com", "999999", "654321");
        });

        assertEquals("Kode verifikasi salah atau telah kedaluwarsa", exception.getMessage());
        verify(balanceRepository, never()).save(any());
    }
}

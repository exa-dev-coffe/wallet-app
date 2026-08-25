package com.wallet_service.be.balance;

import com.wallet_service.be.balance.dto.GetBalanceResponseDto;
import com.wallet_service.be.balance.dto.TopUpResponseDto;
import com.wallet_service.be.balance.projection.BalanceProjection;
import com.wallet_service.be.balanceHistory.BalancehistoryModel;
import com.wallet_service.be.balanceHistory.BalancehistoryService;
import com.wallet_service.be.balanceHistory.enums.StatusBalanceHistory;
import com.wallet_service.be.balanceHistory.enums.TypeBalanceHistory;
import com.wallet_service.be.exception.BadRequestException;
import com.wallet_service.be.exception.NotFoundException;
import com.wallet_service.be.exception.TooManyRequestException;
import com.wallet_service.be.lib.MidtransService;
import com.wallet_service.be.lib.RabbitmqService;
import com.wallet_service.be.utils.PasswordUtils;
import com.wallet_service.be.utils.commons.MidtransChargeRequestDto;
import com.wallet_service.be.utils.commons.MidtransChargeResponseDto;
import com.wallet_service.be.utils.commons.MidtransRequestDto;
import com.wallet_service.be.utils.commons.MidtransResponseDto;
import com.wallet_service.be.utils.commons.ResponseModel;
import com.wallet_service.be.utils.enums.ExchangeType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import jakarta.transaction.Transactional;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.UUID;



@Service
@Slf4j
public class BalanceService {

    private final BalanceRepository balanceRepository;
    private final MidtransService midtransService;
    private final BalancehistoryService balancehistoryService;
    private final RabbitmqService rabbitmqService;
    private final RedisTemplate<String, Object> redisTemplate;

    public BalanceService(BalanceRepository balanceRepository, MidtransService midtransService, BalancehistoryService balancehistoryService, RabbitmqService rabbitmqService, RedisTemplate<String, Object> redisTemplate) {
        this.balanceRepository = balanceRepository;
        this.balancehistoryService = balancehistoryService;
        this.midtransService = midtransService;
        this.rabbitmqService = rabbitmqService;
        this.redisTemplate = redisTemplate;
    }

    public ResponseEntity<ResponseModel<GetBalanceResponseDto>> getBalanceByUserId(int userId) {
        BalanceProjection balance = balanceRepository.findByUserId(userId, BalanceProjection.class);
        if (balance == null) {
            throw new NotFoundException("Balance not found");
        }

        GetBalanceResponseDto responseData = new GetBalanceResponseDto();
        responseData.setIsActive(balance.getIsActive());
        responseData.setBalance(balance.getBalance());
        ResponseModel<GetBalanceResponseDto> response = new ResponseModel<>(true, "Balance retrieved successfully", responseData);
        return ResponseEntity.ok(response);
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public ResponseEntity<ResponseModel<String>> setPin(int userId, String pin) {
        BalanceModel balance = balanceRepository.findByUserId(userId);
        if (balance != null) {
            throw new BadRequestException("Pin already exists");
        }
        BalanceModel newBalance = new BalanceModel();
        newBalance.setUserId(userId);
        newBalance.setPin(PasswordUtils.hashPassword(pin));
        newBalance.setBalance(0.0);
        newBalance.setActive(true);
        newBalance.setCreatedBy(userId);
        balanceRepository.save(newBalance);
        ResponseModel<String> response = new ResponseModel<>(true, "Pin set successfully", null);
        return ResponseEntity.ok(response);
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public ResponseEntity<ResponseModel<String>> changePin(int userId, String oldPin, String newPin) {
        BalanceModel balance = balanceRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new NotFoundException("Wallet not found"));
        if (!balance.isActive()) {
            throw new NotFoundException("Wallet not activated");
        }

        if (!PasswordUtils.matches(oldPin, balance.getPin())) {
            throw new BadRequestException("Current PIN is incorrect");
        }

        balance.setPin(PasswordUtils.hashPassword(newPin));
        balanceRepository.save(balance);

        ResponseModel<String> response = new ResponseModel<>(true, "PIN changed successfully", null);
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<ResponseModel<String>> sendResetPinCode(int userId, String email) throws Exception {
        BalanceModel balance = balanceRepository.findByUserId(userId);
        if (balance == null || !balance.isActive()) {
            throw new NotFoundException("Wallet not activated");
        }

        if (email == null || email.trim().isEmpty()) {
            throw new BadRequestException("Email not found");
        }

        String sendCountKey = "wallet:resetPin:sendCount:" + email;
        Object countObj = redisTemplate.opsForValue().get(sendCountKey);
        int count = 0;
        if (countObj != null) {
            if (countObj instanceof Integer) {
                count = (Integer) countObj;
            } else if (countObj instanceof Long) {
                count = ((Long) countObj).intValue();
            } else {
                count = Integer.parseInt(countObj.toString());
            }
        }

        if (count >= 3) {
            throw new TooManyRequestException("Maximum limit of PIN verification code requests (3 times) for today has been reached.");
        }

        // Generate 6-digit OTP code
        String code = String.format("%06d", (int) (Math.random() * 1000000));

        // Store count with 24 hours expiry
        if (countObj == null) {
            redisTemplate.opsForValue().set(sendCountKey, 1, java.time.Duration.ofHours(24));
        } else {
            redisTemplate.opsForValue().set(sendCountKey, count + 1, java.time.Duration.ofHours(24));
        }

        // Store code in Redis with 10 mins expiry
        String codeKey = "wallet:resetPin:code:" + email;
        redisTemplate.opsForValue().set(codeKey, code, java.time.Duration.ofMinutes(10));

        // Publish to rabbitmq queue emailQueue.resetPin
        String jsonMessage = String.format("{\"to\":\"%s\",\"subject\":\"Wallet PIN Reset Verification Code - Diskusi Coffee\",\"code\":\"%s\"}", email, code);
        this.rabbitmqService.sendMessage(
                "Email Reset PIN Code",
                "emailQueue.resetPin",
                "email.queue",
                ExchangeType.DIRECT,
                null,
                jsonMessage,
                true,
                false,
                false,
                null
        );

        ResponseModel<String> response = new ResponseModel<>(true, "PIN reset verification code has been sent to your email.", null);
        return ResponseEntity.ok(response);
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public ResponseEntity<ResponseModel<String>> resetPin(int userId, String email, String code, String newPin) {
        BalanceModel balance = balanceRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new NotFoundException("Wallet not found"));
        if (!balance.isActive()) {
            throw new NotFoundException("Wallet not activated");
        }

        if (code == null || code.trim().isEmpty()) {
            throw new BadRequestException("Verification code is required");
        }

        String codeKey = "wallet:resetPin:code:" + email;
        Object savedCode = redisTemplate.opsForValue().get(codeKey);
        if (savedCode == null || !savedCode.toString().equals(code)) {
            throw new BadRequestException("Verification code is incorrect or has expired");
        }

        balance.setPin(PasswordUtils.hashPassword(newPin));
        balanceRepository.save(balance);

        redisTemplate.delete(codeKey);

        ResponseModel<String> response = new ResponseModel<>(true, "Wallet PIN updated successfully", null);
        return ResponseEntity.ok(response);
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public ResponseEntity<ResponseModel<TopUpResponseDto>> topUp(int userId, double amount, String paymentType, String bank, String email, String fullName) throws Exception {
        BalanceModel balance = balanceRepository.findByUserId(userId);
        if (balance == null) {
            throw new NotFoundException("Balance not found");
        }

        UUID balanceHistoryId = balancehistoryService.createBalanceHistory(balance, TypeBalanceHistory.TOPUP, amount, null, null, StatusBalanceHistory.PENDING, email, fullName);


        MidtransChargeRequestDto chargeRequestDto = MidtransChargeRequestDto.builder()
                .orderId(balanceHistoryId)
                .grossAmount(amount)
                .firstName(fullName)
                .email(email)
                .paymentType(paymentType)
                .bank(bank)
                .build();

        MidtransChargeResponseDto res = midtransService.chargeTransaction(chargeRequestDto);

        if (res != null) {
            balancehistoryService.updateCoreApiPaymentDetails(
                    balanceHistoryId,
                    res.getPaymentType(),
                    res.getBank(),
                    res.getVaNumber(),
                    res.getBillKey(),
                    res.getBillerCode(),
                    res.getQrUrl(),
                    res.getQrString(),
                    res.getDeeplinkUrl(),
                    res.getExpiryTime()
            );
        }

        balancehistoryService.publishBalanceHistoryUpdate(balanceHistoryId, StatusBalanceHistory.PENDING, userId);

        TopUpResponseDto responseData = TopUpResponseDto.builder()
                .balanceHistoryId(balanceHistoryId)
                .amount(amount)
                .paymentType(res != null ? res.getPaymentType() : paymentType)
                .transactionStatus(res != null ? res.getTransactionStatus() : "pending")
                .transactionId(res != null ? res.getTransactionId() : null)
                .bank(res != null ? res.getBank() : bank)
                .vaNumber(res != null ? res.getVaNumber() : null)
                .billKey(res != null ? res.getBillKey() : null)
                .billerCode(res != null ? res.getBillerCode() : null)
                .qrUrl(res != null ? res.getQrUrl() : null)
                .qrString(res != null ? res.getQrString() : null)
                .deeplinkUrl(res != null ? res.getDeeplinkUrl() : null)
                .expiryTime(res != null ? res.getExpiryTime() : null)
                .userEmail(email)
                .userName(fullName)
                .build();


        ResponseModel<TopUpResponseDto> response = new ResponseModel<>(true, "Top up initiated successfully", responseData);
        return ResponseEntity.ok(response);
    }


    @Transactional(Transactional.TxType.REQUIRED)
    public ResponseEntity<ResponseModel<String>> notificationMidtransHandler(String orderId, StatusBalanceHistory statusBalanceHistory, String statusCode, String grossAmount, String signatureKey) throws Exception {
        boolean isValidSignature = midtransService.validateSignatureKey(
                orderId, statusCode, grossAmount, signatureKey
        );
        if (!isValidSignature) {
            throw new BadRequestException("Invalid signature key");
        }

        // 1. Handle POS QRIS Payment (Order ID starts with "POS-")
        if (orderId != null && orderId.startsWith("POS-")) {
            if (statusBalanceHistory == StatusBalanceHistory.COMPLETED) {
                try {
                    Map<String, Object> posPayload = new HashMap<>();
                    posPayload.put("orderRef", orderId);
                    posPayload.put("paymentStatus", "PAID");
                    posPayload.put("grossAmount", Double.parseDouble(grossAmount));
                    posPayload.put("timestamp", Instant.now().toString());

                    ObjectMapper mapper = new ObjectMapper();
                    String jsonMessage = mapper.writeValueAsString(posPayload);

                    rabbitmqService.sendToExchange(
                            "pos.payment.settled",
                            ExchangeType.DIRECT,
                            "",
                            jsonMessage,
                            true,
                            false,
                            null
                    );
                    log.info("Published POS QRIS settlement to RabbitMQ for orderRef: {}", orderId);
                } catch (Exception e) {
                    log.error("Failed to publish POS QRIS settlement to RabbitMQ", e);
                }
            }
            return ResponseEntity.ok(new ResponseModel<>(true, "POS notification processed", null));
        }

        // 2. Handle Wallet Top-Up (UUID or TOPUP-UUID)
        String cleanId = orderId != null && orderId.startsWith("TOPUP-") ? orderId.substring(6) : orderId;
        UUID id;
        try {
            id = UUID.fromString(cleanId);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid order ID format");
        }

        BalancehistoryModel balancehistoryModel = balancehistoryService.getBalanceHistoryById(id);
        if (balancehistoryModel == null) {
            throw new NotFoundException("Transaction not found");
        }

        // Idempotency: Prevent double processing if already completed or failed
        if (balancehistoryModel.getStatus() == StatusBalanceHistory.COMPLETED || balancehistoryModel.getStatus() == StatusBalanceHistory.FAILED) {
            return ResponseEntity.ok(new ResponseModel<>(true, "Notification already processed", null));
        }

        BalanceModel balance = balanceRepository.findByIdForUpdate(balancehistoryModel.getBalance().getId())
                .orElseThrow(() -> new NotFoundException("Balance not found"));

        if (statusBalanceHistory != StatusBalanceHistory.COMPLETED) {
            balancehistoryService.updateBalanceHistoryStatus(id, statusBalanceHistory, balance.getUserId());
            return ResponseEntity.ok(new ResponseModel<>(true, "Notification processed", null));
        }
        balance.setBalance(balance.getBalance() + balancehistoryModel.getAmount());
        balanceRepository.save(balance);

        balancehistoryService.updateBalanceHistoryStatus(id, statusBalanceHistory, balance.getUserId());

        // Publish receipt to RabbitMQ for email-service if this is a top-up
        if (balancehistoryModel.getType() == TypeBalanceHistory.TOPUP && balancehistoryModel.getUserEmail() != null) {
            try {
                Map<String, Object> emailPayload = new HashMap<>();
                emailPayload.put("to", balancehistoryModel.getUserEmail());
                emailPayload.put("subject", "Coffe - Wallet Top Up Receipt");
                emailPayload.put("userName", balancehistoryModel.getUserName() != null ? balancehistoryModel.getUserName() : "Customer");
                emailPayload.put("amount", balancehistoryModel.getAmount());
                emailPayload.put("paymentType", balancehistoryModel.getPaymentType() != null ? balancehistoryModel.getPaymentType() : "Core API");
                emailPayload.put("bank", balancehistoryModel.getBank() != null ? balancehistoryModel.getBank() : "-");
                emailPayload.put("transactionId", balancehistoryModel.getId().toString());
                emailPayload.put("date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

                ObjectMapper mapper = new ObjectMapper();
                String jsonMessage = mapper.writeValueAsString(emailPayload);

                rabbitmqService.sendToExchange(
                        "email.queue",
                        ExchangeType.DIRECT,
                        "emailQueue.topupReceipt",
                        jsonMessage,
                        true,
                        false,
                        null
                );
                log.info("Published top-up receipt to RabbitMQ for transaction {}", balancehistoryModel.getId());
            } catch (Exception e) {
                log.error("Failed to publish email receipt to RabbitMQ for transaction " + balancehistoryModel.getId(), e);
            }
        }

        return ResponseEntity.ok(new ResponseModel<>(true, "Notification processed", null));
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public boolean pay(int userId, double amount, String pin) {
        BalanceModel balance = balanceRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new NotFoundException("Balance not found"));
        if (!balance.isActive()) {
            throw new BadRequestException("Wallet not activated");
        }
        if (!PasswordUtils.matches(pin, balance.getPin())) {
            throw new BadRequestException("Invalid pin");
        }
        if (balance.getBalance() < amount) {
            return false;
        } else {
            balance.setBalance(balance.getBalance() - amount);
            balanceRepository.save(balance);
            balancehistoryService.createBalanceHistory(balance, TypeBalanceHistory.PAYMENT, amount, null, null, StatusBalanceHistory.COMPLETED);
            return true;
        }
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public ResponseEntity<ResponseModel<String>> syncTransactionStatus(UUID id, int userId) throws Exception {
        BalancehistoryModel balancehistoryModel = balancehistoryService.getBalanceHistoryById(id);
        if (balancehistoryModel == null || balancehistoryModel.getBalance().getUserId() != userId) {
            throw new NotFoundException("Transaction not found");
        }

        if (balancehistoryModel.getStatus() == StatusBalanceHistory.COMPLETED || balancehistoryModel.getStatus() == StatusBalanceHistory.FAILED) {
            return ResponseEntity.ok(new ResponseModel<>(true, "Transaction already synced", null));
        }

        JSONObject midtransResponse = midtransService.checkTransactionStatus(id.toString());
        String statusCode = midtransResponse.optString("status_code", "");
        if (statusCode.equals("404")) {
            return ResponseEntity.ok(new ResponseModel<>(true, "Transaction not found on Midtrans", null));
        }

        String transactionStatus = midtransResponse.optString("transaction_status", "pending");
        String fraudStatus = midtransResponse.optString("fraud_status", "accept");

        StatusBalanceHistory statusBalanceHistory = midtransService.mapTransactionStatus(transactionStatus, fraudStatus);

        BalanceModel balance = balanceRepository.findByIdForUpdate(balancehistoryModel.getBalance().getId())
                .orElseThrow(() -> new NotFoundException("Balance not found"));

        if (statusBalanceHistory != StatusBalanceHistory.COMPLETED) {
            balancehistoryService.updateBalanceHistoryStatus(id, statusBalanceHistory, balance.getUserId());
            return ResponseEntity.ok(new ResponseModel<>(true, "Sync successful, status updated to " + statusBalanceHistory.name(), null));
        }

        balance.setBalance(balance.getBalance() + balancehistoryModel.getAmount());
        balanceRepository.save(balance);

        balancehistoryService.updateBalanceHistoryStatus(id, statusBalanceHistory, balance.getUserId());

        if (balancehistoryModel.getType() == TypeBalanceHistory.TOPUP && balancehistoryModel.getUserEmail() != null) {
            try {
                Map<String, Object> emailPayload = new HashMap<>();
                emailPayload.put("to", balancehistoryModel.getUserEmail());
                emailPayload.put("subject", "Coffe - Wallet Top Up Receipt");
                emailPayload.put("userName", balancehistoryModel.getUserName() != null ? balancehistoryModel.getUserName() : "Customer");
                emailPayload.put("amount", balancehistoryModel.getAmount());
                emailPayload.put("paymentType", balancehistoryModel.getPaymentType() != null ? balancehistoryModel.getPaymentType() : "Core API");
                emailPayload.put("bank", balancehistoryModel.getBank() != null ? balancehistoryModel.getBank() : "-");
                emailPayload.put("transactionId", balancehistoryModel.getId().toString());
                emailPayload.put("date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

                ObjectMapper mapper = new ObjectMapper();
                String jsonMessage = mapper.writeValueAsString(emailPayload);

                rabbitmqService.sendToExchange(
                        "email.queue",
                        ExchangeType.DIRECT,
                        "emailQueue.topupReceipt",
                        jsonMessage,
                        true,
                        false,
                        null
                );
                log.info("Published top-up receipt to RabbitMQ for transaction {}", balancehistoryModel.getId());
            } catch (Exception e) {
                log.error("Failed to publish email receipt to RabbitMQ for transaction " + balancehistoryModel.getId(), e);
            }
        }

        return ResponseEntity.ok(new ResponseModel<>(true, "Sync successful, status updated to COMPLETED", null));
    }

    public ResponseEntity<ResponseModel<com.wallet_service.be.balance.dto.GeneratePosCodeResponseDto>> generatePosPaymentCode(int userId, String pin, String email, String fullName) {
        BalanceModel balance = balanceRepository.findByUserId(userId);
        if (balance == null || !balance.isActive()) {
            throw new NotFoundException("Wallet not activated");
        }

        if (!PasswordUtils.matches(pin, balance.getPin())) {
            throw new BadRequestException("Invalid PIN");
        }

        // Generate 6-digit payment code (e.g. 100000 - 999999)
        String code = String.format("%06d", (int) (100000 + Math.random() * 900000));
        String codeKey = "wallet:pos_code:" + code;

        // Store payload in Redis with 5 minutes (300 seconds) expiry
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("email", email != null ? email : "");
        payload.put("fullName", fullName != null ? fullName : "");

        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonPayload = mapper.writeValueAsString(payload);
            redisTemplate.opsForValue().set(codeKey, jsonPayload, java.time.Duration.ofMinutes(5));
        } catch (Exception e) {
            log.error("Failed to serialize POS payment code payload", e);
            throw new BadRequestException("Failed to generate payment code");
        }

        com.wallet_service.be.balance.dto.GeneratePosCodeResponseDto responseData = com.wallet_service.be.balance.dto.GeneratePosCodeResponseDto.builder()
                .paymentCode(code)
                .expiresInSeconds(300)
                .currentBalance(balance.getBalance())
                .userName(fullName)
                .userEmail(email)
                .build();

        return ResponseEntity.ok(new ResponseModel<>(true, "Payment code generated successfully", responseData));
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public com.wallet_service.be.internal.dto.PosWalletPayResponseDto payWithPosCode(String paymentCode, double amount, Long orderId) {
        if (paymentCode == null || paymentCode.trim().isEmpty()) {
            throw new BadRequestException("Payment code is required");
        }

        String codeKey = "wallet:pos_code:" + paymentCode.trim();
        Object payloadObj = redisTemplate.opsForValue().get(codeKey);
        if (payloadObj == null) {
            throw new BadRequestException("Invalid or expired payment code. Please ask customer to generate a new code.");
        }

        int userId;
        String email = "";
        String fullName = "Customer";
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> payloadMap = mapper.readValue(payloadObj.toString(), Map.class);
            userId = ((Number) payloadMap.get("userId")).intValue();
            if (payloadMap.get("email") != null) email = payloadMap.get("email").toString();
            if (payloadMap.get("fullName") != null) fullName = payloadMap.get("fullName").toString();
        } catch (Exception e) {
            log.error("Failed to parse POS payment code payload", e);
            throw new BadRequestException("Failed to process payment code");
        }

        BalanceModel balance = balanceRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new NotFoundException("Customer wallet not found"));
        if (!balance.isActive()) {
            throw new BadRequestException("Customer wallet is inactive");
        }

        if (balance.getBalance() < amount) {
            throw new BadRequestException(String.format("Insufficient customer wallet balance. Current: Rp %.2f, Required: Rp %.2f", balance.getBalance(), amount));
        }

        balance.setBalance(balance.getBalance() - amount);
        balanceRepository.save(balance);

        UUID historyId = balancehistoryService.createBalanceHistory(balance, TypeBalanceHistory.PAYMENT, amount, null, null, StatusBalanceHistory.COMPLETED, email, fullName);

        // Publish RabbitMQ SSE event so customer frontend updates wallet balance & history in real-time
        try {
            balancehistoryService.publishBalanceHistoryUpdate(historyId, StatusBalanceHistory.COMPLETED, userId);
        } catch (Exception e) {
            log.error("Failed to publish balance history SSE update event for userId={}", userId, e);
        }

        // Delete (burn) payment code immediately so it cannot be re-used
        redisTemplate.delete(codeKey);

        return com.wallet_service.be.internal.dto.PosWalletPayResponseDto.builder()
                .success(true)
                .userId(userId)
                .customerName(fullName)
                .customerEmail(email)
                .amountPaid(amount)
                .remainingBalance(balance.getBalance())
                .message("Payment successful")
                .build();
    }

    public com.wallet_service.be.internal.dto.PosQrisChargeResponseDto chargePosQris(com.wallet_service.be.internal.dto.PosQrisChargeRequestDto requestDto) throws Exception {
        MidtransChargeRequestDto chargeRequestDto = MidtransChargeRequestDto.builder()
                .customOrderId(requestDto.getOrderId())
                .grossAmount(requestDto.getGrossAmount())
                .firstName(requestDto.getCustomerName() != null ? requestDto.getCustomerName() : "POS Guest")
                .email(requestDto.getCustomerEmail() != null ? requestDto.getCustomerEmail() : "pos@diskusicoffee.com")
                .paymentType("qris")
                .build();

        MidtransChargeResponseDto res = midtransService.chargeTransaction(chargeRequestDto);

        return com.wallet_service.be.internal.dto.PosQrisChargeResponseDto.builder()
                .orderId(requestDto.getOrderId())
                .grossAmount(requestDto.getGrossAmount())
                .qrString(res != null ? res.getQrString() : null)
                .qrUrl(res != null ? res.getQrUrl() : null)
                .expiryTime(res != null ? res.getExpiryTime() : null)
                .transactionStatus(res != null ? res.getTransactionStatus() : "pending")
                .transactionId(res != null ? res.getTransactionId() : null)
                .build();
    }

    public JSONObject checkPosQrisStatus(String orderId) throws Exception {
        return midtransService.checkTransactionStatus(orderId);
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public boolean refundPosWallet(Integer userId, Double amount, Long orderId) {
        if (userId == null || userId <= 0 || amount == null || amount <= 0) {
            return false;
        }
        BalanceModel balance = balanceRepository.findByUserIdForUpdate(userId)
                .orElse(null);
        if (balance == null) {
            return false;
        }
        balance.setBalance(balance.getBalance() + amount);
        balanceRepository.save(balance);
        UUID historyId = balancehistoryService.createBalanceHistory(balance, TypeBalanceHistory.TOPUP, amount, null, null, StatusBalanceHistory.COMPLETED, null, "POS Auto Refund (Order #" + orderId + ")");
        
        try {
            balancehistoryService.publishBalanceHistoryUpdate(historyId, StatusBalanceHistory.COMPLETED, userId);
        } catch (Exception e) {
            log.error("Failed to publish balance history SSE update event for refund userId={}", userId, e);
        }

        log.info("Auto-refunded Rp {} to user {} for failed POS transaction {}", amount, userId, orderId);
        return true;
    }
}

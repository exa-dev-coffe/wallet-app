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
        BalanceModel balance = balanceRepository.findByUserId(userId);
        if (balance == null || !balance.isActive()) {
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
        BalanceModel balance = balanceRepository.findByUserId(userId);
        if (balance == null || !balance.isActive()) {
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
    public ResponseEntity<ResponseModel<String>> notificationMidtransHandler(UUID id, StatusBalanceHistory statusBalanceHistory, String statusCode, String grossAmount, String signatureKey) throws Exception {
        boolean isValidSignature = midtransService.validateSignatureKey(
                id, statusCode, grossAmount, signatureKey

        );
        if (!isValidSignature) {
            throw new BadRequestException("Invalid signature key");
        }

        BalancehistoryModel balancehistoryModel = balancehistoryService.getBalanceHistoryById(id);

        BalanceModel balance = balanceRepository.findById(balancehistoryModel.getBalance().getId()).orElse(null);

        if (balance == null) {
            throw new NotFoundException("Balance not found");
        }

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

    public boolean pay(int userId, double amount, String pin) {
        BalanceModel balance = balanceRepository.findByUserId(userId);
        if (balance == null) {
            throw new NotFoundException("Balance not found");
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

        BalanceModel balance = balanceRepository.findById(balancehistoryModel.getBalance().getId()).orElse(null);
        if (balance == null) {
            throw new NotFoundException("Balance not found");
        }

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
}

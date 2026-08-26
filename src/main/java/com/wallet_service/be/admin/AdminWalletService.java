package com.wallet_service.be.admin;

import com.wallet_service.be.admin.dto.AdminResetPinRequestDto;
import com.wallet_service.be.admin.dto.AdminSendResetPinRequestDto;
import com.wallet_service.be.admin.dto.AdminWalletResponseDto;
import com.wallet_service.be.admin.dto.AdminWalletSummaryDto;
import com.wallet_service.be.balance.BalanceModel;
import com.wallet_service.be.balance.BalanceRepository;
import com.wallet_service.be.exception.BadRequestException;
import com.wallet_service.be.exception.NotFoundException;
import com.wallet_service.be.lib.RabbitmqService;
import com.wallet_service.be.utils.HmacUtils;
import com.wallet_service.be.utils.PasswordUtils;
import com.wallet_service.be.utils.commons.PaginationResponseDto;
import com.wallet_service.be.utils.commons.ResponseModel;
import com.wallet_service.be.utils.enums.ExchangeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@lombok.extern.slf4j.Slf4j
@Service
public class AdminWalletService {

    private final BalanceRepository balanceRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RabbitmqService rabbitmqService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private HmacUtils hmacUtils;

    @org.springframework.beans.factory.annotation.Value("${app.account-service.url:http://localhost:8080}")
    private String accountServiceUrl;

    public AdminWalletService(BalanceRepository balanceRepository, RedisTemplate<String, Object> redisTemplate, RabbitmqService rabbitmqService) {
        this.balanceRepository = balanceRepository;
        this.redisTemplate = redisTemplate;
        this.rabbitmqService = rabbitmqService;
    }

    public ResponseEntity<ResponseModel<PaginationResponseDto<AdminWalletResponseDto>>> getAllWallets(Pageable pageable, String search) {
        Specification<BalanceModel> spec = (root, query, cb) -> {
            if (search != null && !search.trim().isEmpty()) {
                String searchTrim = search.trim();
                try {
                    int searchUserId = Integer.parseInt(searchTrim);
                    return cb.or(
                            cb.equal(root.get("userId"), searchUserId),
                            cb.like(cb.lower(root.get("walletNumber")), "%" + searchTrim.toLowerCase() + "%")
                    );
                } catch (NumberFormatException ignored) {
                    return cb.like(cb.lower(root.get("walletNumber")), "%" + searchTrim.toLowerCase() + "%");
                }
            }
            return cb.conjunction();
        };

        Page<BalanceModel> pageResult = balanceRepository.findAll(spec, pageable);

        List<AdminWalletResponseDto> dtoList = pageResult.getContent().stream().map(b ->
                AdminWalletResponseDto.builder()
                        .id(b.getId())
                        .userId(b.getUserId())
                        .walletNumber(b.getWalletNumber())
                        .balance(b.getBalance())
                        .isActive(b.isActive())
                        .createdAt(b.getCreatedAt())
                        .updatedAt(b.getUpdatedAt())
                        .build()
        ).collect(Collectors.toList());

        PaginationResponseDto<AdminWalletResponseDto> pagination = PaginationResponseDto.<AdminWalletResponseDto>builder()
                .data(dtoList)
                .totalData(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .currentPage(pageResult.getNumber() + 1)
                .pageSize(pageResult.getSize())
                .isLastPage(pageResult.isLast())
                .build();

        ResponseModel<PaginationResponseDto<AdminWalletResponseDto>> response = new ResponseModel<>(true, "Customer wallets loaded successfully", pagination);
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<ResponseModel<AdminWalletSummaryDto>> getWalletSummary() {
        List<BalanceModel> allBalances = balanceRepository.findAll();
        long activeCount = allBalances.stream().filter(BalanceModel::isActive).count();
        long inactiveCount = allBalances.size() - activeCount;
        double totalBalance = allBalances.stream().mapToDouble(BalanceModel::getBalance).sum();

        AdminWalletSummaryDto summary = AdminWalletSummaryDto.builder()
                .totalActiveWallets(activeCount)
                .totalInactiveWallets(inactiveCount)
                .totalOutstandingBalance(totalBalance)
                .build();

        ResponseModel<AdminWalletSummaryDto> response = new ResponseModel<>(true, "Admin wallet summary loaded successfully", summary);
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<ResponseModel<String>> sendResetPinCodeToEmail(String email) {
        AdminSendResetPinRequestDto req = new AdminSendResetPinRequestDto();
        req.setEmail(email);
        return sendResetPinCodeToEmail(req);
    }

    public ResponseEntity<ResponseModel<String>> sendResetPinCodeToEmail(AdminSendResetPinRequestDto request) {
        if (request == null || request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new BadRequestException("Customer email is required");
        }
        String cleanEmail = request.getEmail().trim().toLowerCase();

        // 1. Check rate limit first
        String sendCountKey = "wallet:resetPin:sendCount:" + cleanEmail;
        Object sendCountObj = redisTemplate.opsForValue().get(sendCountKey);
        int sendCount = sendCountObj != null ? Integer.parseInt(sendCountObj.toString()) : 0;
        if (sendCount >= 5) {
            throw new BadRequestException("Verification code limit reached for this email today. Please try again tomorrow.");
        }

        // 2. Verify if user exists in account-service (via internal HMAC request)
        Integer foundUserId = lookupUserIdFromAccountService(cleanEmail);
        if (foundUserId == null) {
            throw new BadRequestException("Customer with email " + cleanEmail + " not found.");
        }

        // 2b. Match requested userId with foundUserId if specified
        if (request.getUserId() != null && request.getUserId() > 0 && !request.getUserId().equals(foundUserId)) {
            log.warn("Send PIN code failed for email {}: requested userId ({}) does not match registered userId ({})", cleanEmail, request.getUserId(), foundUserId);
            throw new BadRequestException("Selected wallet User ID (" + request.getUserId() + ") does not match email (" + cleanEmail + ")");
        }

        // 3. Verify if customer has created/activated a wallet
        boolean walletExists = balanceRepository.findByUserId(foundUserId) != null;
        if (!walletExists) {
            throw new BadRequestException("Customer has not created or activated a wallet yet. The customer must activate their wallet first.");
        }

        redisTemplate.opsForValue().set("wallet:resetPin:userId:" + cleanEmail, foundUserId, Duration.ofMinutes(10));

        // 4. Generate 6-digit code
        SecureRandom random = new SecureRandom();
        String code = String.format("%06d", random.nextInt(1000000));

        redisTemplate.opsForValue().set(sendCountKey, sendCount + 1, Duration.ofHours(24));

        String codeKey = "wallet:resetPin:code:" + cleanEmail;
        redisTemplate.opsForValue().set(codeKey, code, Duration.ofMinutes(10));

        // 4. Publish to RabbitMQ
        String jsonMessage = String.format("{\"to\":\"%s\",\"subject\":\"Wallet PIN Reset Verification Code - Diskusi Coffee\",\"code\":\"%s\"}", cleanEmail, code);
        try {
            this.rabbitmqService.sendMessage(
                    "Email Reset PIN Code (Admin Triggered)",
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
        } catch (Exception ignored) {
        }

        ResponseModel<String> response = new ResponseModel<>(true, "Verification code has been sent to customer email (" + cleanEmail + ").", null);
        return ResponseEntity.ok(response);
    }

    private Integer lookupUserIdFromAccountService(String email) {
        if (hmacUtils == null) return null;
        try {
            String encodedEmail = java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8);
            String queryParams = "email=" + encodedEmail;
            String timestamp = java.time.Instant.now().toString();
            String signature = hmacUtils.generateHMAC(queryParams + timestamp);

            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            String url = accountServiceUrl + "/api/internal/user-by-email?" + queryParams;

            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(url))
                    .header("X-Signature", signature)
                    .header("X-Timestamp", timestamp)
                    .GET()
                    .build();

            java.net.http.HttpResponse<String> resp = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());

            log.info("Internal account-service GET user-by-email response - URL: {}, Status: {}, Body: {}", url, resp.statusCode(), resp.body());

            if (resp.statusCode() == 404) {
                throw new NotFoundException("Customer account with email '" + email + "' was not found in account-service.");
            }

            if (resp.statusCode() == 200) {
                com.fasterxml.jackson.databind.JsonNode json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(resp.body());
                if (json.has("data") && json.get("data").has("userId")) {
                    return json.get("data").get("userId").asInt();
                }
            }
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed internal request to account-service for email: {}", email, e);
        }
        return null;
    }

    @Transactional
    public ResponseEntity<ResponseModel<String>> resetCustomerPin(AdminResetPinRequestDto request) {
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new BadRequestException("Customer email is required");
        }
        String cleanEmail = request.getEmail().trim().toLowerCase();

        // 1. Verify OTP code stored in Redis
        String codeKey = "wallet:resetPin:code:" + cleanEmail;
        Object savedCode = redisTemplate.opsForValue().get(codeKey);
        if (savedCode == null || !savedCode.toString().equals(request.getCode().trim())) {
            log.warn("Reset PIN failed for email {}: savedCode in Redis is '{}', requested code is '{}'", cleanEmail, savedCode, request.getCode());
            throw new BadRequestException("Verification code is incorrect or has expired");
        }

        // 2. Resolve target user ID from account-service, Redis session, or request payload
        Integer verifiedUserId = lookupUserIdFromAccountService(cleanEmail);
        if (verifiedUserId == null) {
            Object sessionUserId = redisTemplate.opsForValue().get("wallet:resetPin:userId:" + cleanEmail);
            if (sessionUserId != null) {
                try {
                    verifiedUserId = Integer.parseInt(sessionUserId.toString());
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if (verifiedUserId == null) {
            log.warn("Reset PIN failed for email {}: verifiedUserId is null", cleanEmail);
            throw new BadRequestException("User ID could not be identified for email: " + cleanEmail);
        }

        if (request.getUserId() != null && request.getUserId() > 0 && !request.getUserId().equals(verifiedUserId)) {
            log.warn("Reset PIN failed for email {}: requested userId ({}) does not match verifiedUserId ({})", cleanEmail, request.getUserId(), verifiedUserId);
            throw new BadRequestException("Selected wallet User ID (" + request.getUserId() + ") does not match email (" + cleanEmail + ")");
        }

        int targetUserId = verifiedUserId;

        BalanceModel balance = balanceRepository.findByUserIdForUpdate(targetUserId)
                .orElseThrow(() -> new NotFoundException("Customer wallet not found for user ID: " + targetUserId));

        balance.setPin(PasswordUtils.hashPassword(request.getNewPin().trim()));
        balance.setActive(true);
        balanceRepository.save(balance);

        redisTemplate.delete(codeKey);
        redisTemplate.delete("wallet:resetPin:userId:" + cleanEmail);

        ResponseModel<String> response = new ResponseModel<>(true, "Customer transaction PIN reset successfully", null);
        return ResponseEntity.ok(response);
    }

    @Transactional
    public ResponseEntity<ResponseModel<String>> toggleWalletStatus(int userId) {
        BalanceModel balance = balanceRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new NotFoundException("Wallet not found for user ID: " + userId));

        balance.setActive(!balance.isActive());
        balanceRepository.save(balance);

        String message = balance.isActive() ? "Wallet activated successfully" : "Wallet suspended successfully";
        return ResponseEntity.ok(new ResponseModel<>(true, message, null));
    }
}

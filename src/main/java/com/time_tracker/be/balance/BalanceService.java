package com.time_tracker.be.balance;

import com.time_tracker.be.balance.dto.GetBalanceResponseDto;
import com.time_tracker.be.balance.dto.TopUpResponseDto;
import com.time_tracker.be.balance.projection.BalanceProjection;
import com.time_tracker.be.balanceHistory.BalancehistoryModel;
import com.time_tracker.be.balanceHistory.BalancehistoryService;
import com.time_tracker.be.balanceHistory.enums.StatusBalanceHistory;
import com.time_tracker.be.balanceHistory.enums.TypeBalanceHistory;
import com.time_tracker.be.exception.BadRequestException;
import com.time_tracker.be.exception.NotFoundException;
import com.time_tracker.be.lib.MidtransService;
import com.time_tracker.be.lib.RabbitmqService;
import com.time_tracker.be.utils.PasswordUtils;
import com.time_tracker.be.utils.commons.MidtransRequestDto;
import com.time_tracker.be.utils.commons.MidtransResponseDto;
import com.time_tracker.be.utils.commons.ResponseModel;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class BalanceService {
    private final BalanceRepository balanceRepository;
    private final MidtransService midtransService;
    private final BalancehistoryService balancehistoryService;
    private final RabbitmqService rabbitmqService;

    public BalanceService(BalanceRepository balanceRepository, MidtransService midtransService, BalancehistoryService balancehistoryService, RabbitmqService rabbitmqService) {
        this.balanceRepository = balanceRepository;
        this.balancehistoryService = balancehistoryService;
        this.midtransService = midtransService;
        this.rabbitmqService = rabbitmqService;
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
    public ResponseEntity<ResponseModel<TopUpResponseDto>> topUp(int userId, double amount, String pin, String email, String fullName) throws Exception {
        BalanceModel balance = balanceRepository.findByUserId(userId);
        if (balance == null) {
            throw new NotFoundException("Balance not found");
        }
        if (!PasswordUtils.matches(pin, balance.getPin())) {
            throw new BadRequestException("Invalid pin");
        }

        Integer balanceHistoryId = balancehistoryService.createBalanceHistory(balance, TypeBalanceHistory.TOPUP, amount, null, null);

        MidtransRequestDto midtransRequestDto = new MidtransRequestDto(balanceHistoryId, amount, fullName, email);

        MidtransResponseDto res = midtransService.createTransaction(midtransRequestDto);
        if (res != null) {
            balancehistoryService.updateMidtransTokenAndRedirectUrl(balanceHistoryId, res.getToken(), res.getRedirectUrl());
        }

        TopUpResponseDto responseData = new TopUpResponseDto(res.getRedirectUrl(), res.getToken());
        ResponseModel<TopUpResponseDto> response = new ResponseModel<>(true, "Top up initiated successfully", responseData);
        return ResponseEntity.ok(response);
    }

    @Transactional(Transactional.TxType.REQUIRED)
    public ResponseEntity<ResponseModel<String>> notificationMidtransHandler(String id, StatusBalanceHistory statusBalanceHistory, String statusCode, String grossAmount, String signatureKey) throws Exception {
        boolean isValidSignature = midtransService.validateSignatureKey(
                id, statusCode, grossAmount, signatureKey

        );
        if (!isValidSignature) {
            throw new BadRequestException("Invalid signature key");
        }

        BalancehistoryModel balancehistoryModel = balancehistoryService.getBalanceHistoryById(Integer.valueOf(id));

        BalanceModel balance = balanceRepository.findById(balancehistoryModel.getBalance().getId()).orElse(null);

        if (balance == null) {
            throw new NotFoundException("Balance not found");
        }

        if (statusBalanceHistory != StatusBalanceHistory.COMPLETED) {
            balancehistoryService.updateBalanceHistoryStatus(Integer.valueOf(id), statusBalanceHistory, balance.getUserId());
            return ResponseEntity.ok(new ResponseModel<>(true, "Notification processed", null));
        }
        balance.setBalance(balance.getBalance() + balancehistoryModel.getAmount());
        balanceRepository.save(balance);

        balancehistoryService.updateBalanceHistoryStatus(Integer.valueOf(id), statusBalanceHistory, balance.getUserId());

        return ResponseEntity.ok(new ResponseModel<>(true, "Notification processed", null));
    }

}

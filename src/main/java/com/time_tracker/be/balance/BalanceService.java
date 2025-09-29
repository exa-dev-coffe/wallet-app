package com.time_tracker.be.balance;

import com.time_tracker.be.balance.dto.GetBalanceResponseDto;
import com.time_tracker.be.balance.projection.BalanceProjection;
import com.time_tracker.be.exception.BadRequestException;
import com.time_tracker.be.exception.NotFoundException;
import com.time_tracker.be.utils.PasswordUtils;
import com.time_tracker.be.utils.commons.ResponseModel;
import jakarta.transaction.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class BalanceService {
    private final BalanceRepository balanceRepository;

    public BalanceService(BalanceRepository balanceRepository) {
        this.balanceRepository = balanceRepository;
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
}

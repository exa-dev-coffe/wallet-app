package com.wallet_service.be.balanceHistory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet_service.be.balance.BalanceModel;
import com.wallet_service.be.balanceHistory.dto.BalanceHistoryPayloadDto;
import com.wallet_service.be.balanceHistory.dto.BalanceHistoryResponseDto;
import com.wallet_service.be.balanceHistory.enums.StatusBalanceHistory;
import com.wallet_service.be.balanceHistory.enums.TypeBalanceHistory;
import com.wallet_service.be.exception.BadRequestException;
import com.wallet_service.be.lib.RabbitmqService;
import com.wallet_service.be.utils.commons.GenericSpecification;
import com.wallet_service.be.utils.commons.PaginationResponseDto;
import com.wallet_service.be.utils.commons.ResponseModel;
import com.wallet_service.be.utils.enums.ExchangeType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Service
public class BalancehistoryService {

    private final BalancehistoryRepository balancehistoryRepository;
    private final RabbitmqService rabbitmqService;


    public BalancehistoryService(BalancehistoryRepository balancehistoryRepository, RabbitmqService rabbitmqService) {
        this.balancehistoryRepository = balancehistoryRepository;
        this.rabbitmqService = rabbitmqService;
    }

    public ResponseEntity<ResponseModel<PaginationResponseDto<BalanceHistoryResponseDto>>> getAllBalanceHistory(
            Pageable pageable,
            String searchValue, String searchKey, Integer userId
    ) {
        Specification<BalancehistoryModel> spec = Specification.where(
                (root, query, criteriaBuilder) -> {
                    Predicate userPredicate = criteriaBuilder.equal(root.get("balance").get("userId"), userId);
                    Predicate dynamicPredicate = GenericSpecification.<BalancehistoryModel>dynamicFilter(
                            searchKey, searchValue
                    ).toPredicate(root, query, criteriaBuilder);

                    return criteriaBuilder.and(userPredicate, dynamicPredicate);
                }
        );

        Page<BalancehistoryModel> data = balancehistoryRepository.findAll(spec, pageable);
        Page<BalanceHistoryResponseDto> responseData = data.map(BalanceHistoryResponseDto::fromEntity);

        PaginationResponseDto<BalanceHistoryResponseDto> responsePagination = PaginationResponseDto.fromEntity(responseData);

        ResponseModel<PaginationResponseDto<BalanceHistoryResponseDto>> response = new ResponseModel<>(true, "Data Balance history ditemukan", responsePagination);
        return ResponseEntity.ok(response);
    }

    public UUID createBalanceHistory(BalanceModel balance, TypeBalanceHistory typeBalanceHistory, Double amount, String token, String redirectUrl, StatusBalanceHistory
            statusBalanceHistory) {
        BalancehistoryModel balancehistoryModel = new BalancehistoryModel();
        balancehistoryModel.setBalance(balance);
        balancehistoryModel.setType(typeBalanceHistory);
        balancehistoryModel.setAmount(amount);
        balancehistoryModel.setStatus(statusBalanceHistory);
        balancehistoryModel.setToken(token);
        balancehistoryModel.setRedirectUrl(redirectUrl);
        balancehistoryModel.setCreatedBy(balance.getUserId());
        balancehistoryModel.setUpdatedBy(balance.getUserId());
        balancehistoryRepository.save(balancehistoryModel);
        return balancehistoryModel.getId();
    }

    public void updateMidtransTokenAndRedirectUrl(UUID id, String token, String redirectUrl) {
        BalancehistoryModel balancehistoryModel = balancehistoryRepository.findById(id).orElse(null);
        if (balancehistoryModel != null) {
            balancehistoryModel.setToken(token);
            balancehistoryModel.setRedirectUrl(redirectUrl);
            balancehistoryModel.setUpdatedAt(new Date());
            balancehistoryRepository.save(balancehistoryModel);
        }
    }

    public void publishBalanceHistoryUpdate(UUID balanceHistoryId, StatusBalanceHistory status, Integer userId) throws Exception {
        BalanceHistoryPayloadDto payload = new BalanceHistoryPayloadDto();
        payload.setType("update_balance_history");
        payload.setStatus(status.name());
        payload.setBalanceHistoryId(balanceHistoryId);
        payload.setUserId(userId);

        ObjectMapper mapper = new ObjectMapper();
        String jsonMessage = mapper.writeValueAsString(payload);

        rabbitmqService.sendToExchange(
                "balance.history.updated",  // exchange
                ExchangeType.DIRECT,
                String.valueOf(userId),
                jsonMessage,
                false,
                true,
                null
        );
    }

    public void updateBalanceHistoryStatus(UUID id, StatusBalanceHistory statusBalanceHistory, Integer userId) throws Exception {
        BalancehistoryModel balancehistoryModel = balancehistoryRepository.findById(id).orElse(null);
        if (balancehistoryModel == null) {
            throw new BadRequestException("Balance history not found");
        }
        if (balancehistoryModel.getStatus() == StatusBalanceHistory.PENDING) {
            balancehistoryModel.setStatus(statusBalanceHistory);
            balancehistoryModel.setUpdatedAt(new Date());
            balancehistoryModel.setUpdatedBy(userId);
            balancehistoryRepository.save(balancehistoryModel);
            
            publishBalanceHistoryUpdate(balancehistoryModel.getId(), statusBalanceHistory, userId);
        } else {
            throw new BadRequestException("Balance history already processed");
        }
    }

    public BalancehistoryModel getBalanceHistoryById(UUID id) {
        BalancehistoryModel balancehistoryModel = balancehistoryRepository.findById(id).orElse(null);
        if (balancehistoryModel == null) {
            throw new BadRequestException("Balance history not found");
        }
        return balancehistoryModel;
    }

}

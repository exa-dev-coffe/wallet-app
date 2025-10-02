package com.time_tracker.be.balanceHistory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.time_tracker.be.balance.BalanceModel;
import com.time_tracker.be.balanceHistory.dto.BalanceHistoryPayloadDto;
import com.time_tracker.be.balanceHistory.dto.BalanceHistoryResponseDto;
import com.time_tracker.be.balanceHistory.enums.StatusBalanceHistory;
import com.time_tracker.be.balanceHistory.enums.TypeBalanceHistory;
import com.time_tracker.be.exception.BadRequestException;
import com.time_tracker.be.lib.MidtransService;
import com.time_tracker.be.lib.RabbitmqService;
import com.time_tracker.be.utils.commons.GenericSpecification;
import com.time_tracker.be.utils.commons.PaginationResponseDto;
import com.time_tracker.be.utils.commons.ResponseModel;
import com.time_tracker.be.utils.enums.ExchangeType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class BalancehistoryService {

    private final BalancehistoryRepository balancehistoryRepository;
    private final RabbitmqService rabbitmqService;
    private final MidtransService midtransService;


    public BalancehistoryService(BalancehistoryRepository balancehistoryRepository, RabbitmqService rabbitmqService, MidtransService midtransService) {
        this.balancehistoryRepository = balancehistoryRepository;
        this.midtransService = midtransService;
        this.rabbitmqService = rabbitmqService;
    }

    public ResponseEntity<ResponseModel<PaginationResponseDto<BalanceHistoryResponseDto>>> getAllBalanceHistory(
            Pageable pageable,
            String searchValue, String searchKey
    ) {

        Specification<BalancehistoryModel> spec = Specification.where(
                (root, query, criteriaBuilder) -> {
                    Predicate dynamicPredicate = GenericSpecification.<BalancehistoryModel>dynamicFilter(
                            searchKey, searchValue
                    ).toPredicate(root, query, criteriaBuilder);

                    return criteriaBuilder.and(dynamicPredicate);
                }
        );

        Page<BalancehistoryModel> data = balancehistoryRepository.findAll(spec, pageable);
        Page<BalanceHistoryResponseDto> responseData = data.map(BalanceHistoryResponseDto::fromEntity);

        PaginationResponseDto<BalanceHistoryResponseDto> responsePagination = PaginationResponseDto.fromEntity(responseData);

        ResponseModel<PaginationResponseDto<BalanceHistoryResponseDto>> response = new ResponseModel<>(true, "Data Balance history ditemukan", responsePagination);
        return ResponseEntity.ok(response);
    }

    public Integer createBalanceHistory(BalanceModel balance, TypeBalanceHistory typeBalanceHistory, Double amount, String token, String redirectUrl) {
        BalancehistoryModel balancehistoryModel = new BalancehistoryModel();
        balancehistoryModel.setBalance(balance);
        balancehistoryModel.setType(typeBalanceHistory);
        balancehistoryModel.setAmount(amount);
        balancehistoryModel.setStatus(StatusBalanceHistory.PENDING);
        balancehistoryModel.setToken(token);
        balancehistoryModel.setRedirectUrl(redirectUrl);
        balancehistoryModel.setCreatedBy(balance.getUserId());
        balancehistoryRepository.save(balancehistoryModel);
        return balancehistoryModel.getId();
    }

    public void updateMidtransTokenAndRedirectUrl(Integer id, String token, String redirectUrl) {
        BalancehistoryModel balancehistoryModel = balancehistoryRepository.findById(id).orElse(null);
        if (balancehistoryModel != null) {
            balancehistoryModel.setToken(token);
            balancehistoryModel.setRedirectUrl(redirectUrl);
            balancehistoryRepository.save(balancehistoryModel);
        }
    }

    public void updateBalanceHistoryStatus(Integer id, StatusBalanceHistory statusBalanceHistory) throws Exception {
        BalancehistoryModel balancehistoryModel = balancehistoryRepository.findById(id).orElse(null);
        if (balancehistoryModel != null) {
            balancehistoryModel.setStatus(statusBalanceHistory);
            balancehistoryRepository.save(balancehistoryModel);
            BalanceHistoryPayloadDto payload = new BalanceHistoryPayloadDto();
            payload.setType("update_balance_history");
            payload.setStatus(statusBalanceHistory.name());
            payload.setBalanceHistoryId(balancehistoryModel.getBalance().getId());

            ObjectMapper mapper = new ObjectMapper();
            String jsonMessage = mapper.writeValueAsString(payload);

            // example fanout
            rabbitmqService.sendMessage(
                    "",                         // queueName kosong / abaikan
                    "",                         // routingKey kosong
                    "balance.history.updated",  // exchange
                    ExchangeType.FANOUT,
                    null,
                    jsonMessage,
                    true,
                    false,  // exclusive = false (biarin consumer yang buat)
                    false,
                    null
            );
        } else {
            throw new BadRequestException("Balance history not found");
        }
    }

    public BalancehistoryModel getBalanceHistoryById(Integer id) {
        BalancehistoryModel balancehistoryModel = balancehistoryRepository.findById(id).orElse(null);
        if (balancehistoryModel == null) {
            throw new BadRequestException("Balance history not found");
        }
        return balancehistoryModel;
    }

}

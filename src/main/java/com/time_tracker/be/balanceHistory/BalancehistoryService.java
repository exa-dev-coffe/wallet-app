package com.time_tracker.be.balanceHistory;

import com.time_tracker.be.balanceHistory.dto.BalanceHistoryResponseDto;
import com.time_tracker.be.utils.commons.GenericSpecification;
import com.time_tracker.be.utils.commons.PaginationResponseDto;
import com.time_tracker.be.utils.commons.ResponseModel;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class BalancehistoryService {

    private final BalancehistoryRepository balancehistoryRepository;

    public BalancehistoryService(BalancehistoryRepository balancehistoryRepository) {
        this.balancehistoryRepository = balancehistoryRepository;
    }

    public ResponseEntity<ResponseModel<PaginationResponseDto<BalanceHistoryResponseDto>>> getAllBalanceHistory(
            Pageable pageable,
            String searchValue, String searchKey
    ) {

        Specification<BalancehistoryModel> spec = Specification.where(
                (root, query, criteriaBuilder) -> {
                    Predicate dynamicPredicate = GenericSpecification.<BalancehistoryModel>dynamicFilter(
                            searchValue, searchKey
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


}

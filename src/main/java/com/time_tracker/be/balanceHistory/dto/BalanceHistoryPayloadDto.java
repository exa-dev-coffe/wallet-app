package com.time_tracker.be.balanceHistory.dto;

import com.time_tracker.be.utils.commons.BasePayloadSse;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class BalanceHistoryPayloadDto extends BasePayloadSse {
    private String status;
    private Integer balanceHistoryId;
    private Integer userId;
}

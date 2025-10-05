package com.time_tracker.be.balanceHistory.dto;

import com.time_tracker.be.utils.commons.BasePayloadSse;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
public class BalanceHistoryPayloadDto extends BasePayloadSse {
    private String status;
    private UUID balanceHistoryId;
    private Integer userId;
}

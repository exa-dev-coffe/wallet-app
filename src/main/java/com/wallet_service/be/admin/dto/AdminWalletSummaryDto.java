package com.wallet_service.be.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminWalletSummaryDto {
    private long totalActiveWallets;
    private long totalInactiveWallets;
    private double totalOutstandingBalance;
}

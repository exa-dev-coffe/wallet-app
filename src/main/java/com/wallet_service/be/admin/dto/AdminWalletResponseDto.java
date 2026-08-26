package com.wallet_service.be.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminWalletResponseDto {
    private Integer id;
    private Integer userId;
    private String walletNumber;
    private Double balance;
    private Boolean isActive;
    private Date createdAt;
    private Date updatedAt;
}

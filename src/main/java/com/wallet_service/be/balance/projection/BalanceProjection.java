package com.wallet_service.be.balance.projection;

public interface BalanceProjection {

    Boolean getIsActive();

    Double getBalance();

    String getWalletNumber();
}

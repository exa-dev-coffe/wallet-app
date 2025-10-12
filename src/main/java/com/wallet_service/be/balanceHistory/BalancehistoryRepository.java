package com.wallet_service.be.balanceHistory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BalancehistoryRepository extends JpaRepository<BalancehistoryModel, UUID>, JpaSpecificationExecutor<BalancehistoryModel> {

}

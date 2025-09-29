package com.time_tracker.be.balanceHistory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface BalancehistoryRepository extends JpaRepository<BalancehistoryModel, Integer>, JpaSpecificationExecutor<BalancehistoryModel> {

}

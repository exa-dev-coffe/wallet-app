package com.time_tracker.be.balance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BalanceRepository extends JpaRepository<BalanceModel, Integer> {
    BalanceModel findByUserId(Integer userId);

    <T> T findByUserId(Integer userId, Class<T> type);
}

package com.wallet_service.be.balance;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BalanceRepository extends JpaRepository<BalanceModel, Integer> {
    BalanceModel findByUserId(Integer userId);

    <T> T findByUserId(Integer userId, Class<T> type);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BalanceModel b WHERE b.userId = :userId")
    Optional<BalanceModel> findByUserIdForUpdate(@Param("userId") Integer userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM BalanceModel b WHERE b.id = :id")
    Optional<BalanceModel> findByIdForUpdate(@Param("id") Integer id);
}

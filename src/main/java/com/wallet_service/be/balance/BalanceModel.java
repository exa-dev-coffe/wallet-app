package com.wallet_service.be.balance;

import com.wallet_service.be.utils.commons.BaseModal;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tm_balances")
public class BalanceModel extends BaseModal {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "user_id", nullable = false)
    private int userId;

    @Column(name = "balance", nullable = false, columnDefinition = "DECIMAL(10, 2) DEFAULT 0.00")
    private double balance;

    @Column(name = "is_active", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean isActive;

    @Column(name = "pin", nullable = false)
    private String pin;
}

package com.time_tracker.be.balanceHistory;

import com.time_tracker.be.balance.BalanceModel;
import com.time_tracker.be.balanceHistory.enums.StatusBalanceHistory;
import com.time_tracker.be.balanceHistory.enums.TypeBalanceHistory;
import com.time_tracker.be.utils.commons.BaseModal;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "td_balance_histories")
public class BalancehistoryModel extends BaseModal {
    @Id
    @Column(name = "id")
    @UuidGenerator
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "balance_id", nullable = false)
    private BalanceModel balance;

    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private TypeBalanceHistory type;

    @Column(name = "amount", nullable = false, columnDefinition = "DECIMAL(10, 2) DEFAULT 0.00")
    private double amount;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private StatusBalanceHistory status;

    @Column(name = "token", nullable = true)
    private String token;

    @Column(name = "redirect_url", nullable = true)
    private String redirectUrl;
}

package com.wallet_service.be.balanceHistory;

import com.wallet_service.be.balance.BalanceModel;
import com.wallet_service.be.balanceHistory.enums.StatusBalanceHistory;
import com.wallet_service.be.balanceHistory.enums.TypeBalanceHistory;
import com.wallet_service.be.utils.commons.BaseModal;
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

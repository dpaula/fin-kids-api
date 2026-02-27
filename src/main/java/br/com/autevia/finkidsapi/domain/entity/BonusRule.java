package br.com.autevia.finkidsapi.domain.entity;

import br.com.autevia.finkidsapi.domain.enums.BonusBaseType;
import br.com.autevia.finkidsapi.domain.enums.BonusConditionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "bonus_rules")
public class BonusRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentage;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_type", nullable = false, length = 40)
    private BonusConditionType conditionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "base_type", nullable = false, length = 40)
    private BonusBaseType baseType;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BonusRule() {
    }

    public BonusRule(Account account, BigDecimal percentage, BonusConditionType conditionType, BonusBaseType baseType, boolean active) {
        this.account = account;
        this.percentage = percentage;
        this.conditionType = conditionType;
        this.baseType = baseType;
        this.active = active;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public void setPercentage(BigDecimal percentage) {
        this.percentage = percentage;
    }

    public BonusConditionType getConditionType() {
        return conditionType;
    }

    public void setConditionType(BonusConditionType conditionType) {
        this.conditionType = conditionType;
    }

    public BonusBaseType getBaseType() {
        return baseType;
    }

    public void setBaseType(BonusBaseType baseType) {
        this.baseType = baseType;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

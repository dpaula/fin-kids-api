package br.com.autevia.finkidsapi.integration;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.autevia.finkidsapi.domain.entity.Account;
import br.com.autevia.finkidsapi.domain.entity.AccountTransaction;
import br.com.autevia.finkidsapi.domain.entity.BonusRule;
import br.com.autevia.finkidsapi.domain.entity.AuditEvent;
import br.com.autevia.finkidsapi.domain.enums.AuditActionType;
import br.com.autevia.finkidsapi.domain.enums.BonusBaseType;
import br.com.autevia.finkidsapi.domain.enums.BonusConditionType;
import br.com.autevia.finkidsapi.domain.enums.TransactionOrigin;
import br.com.autevia.finkidsapi.domain.enums.TransactionType;
import br.com.autevia.finkidsapi.repository.AccountRepository;
import br.com.autevia.finkidsapi.repository.AccountTransactionRepository;
import br.com.autevia.finkidsapi.repository.AuditEventRepository;
import br.com.autevia.finkidsapi.repository.BonusRuleRepository;
import br.com.autevia.finkidsapi.service.BonusExecutionService;
import br.com.autevia.finkidsapi.service.dto.bonus.BonusExecutionSummary;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BonusExecutionIntegrationTest {

    @Autowired
    private BonusExecutionService bonusExecutionService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountTransactionRepository accountTransactionRepository;

    @Autowired
    private BonusRuleRepository bonusRuleRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    void shouldApplyMonthlyBonusAndCreateAuditTrail() {
        Account account = accountRepository.save(new Account("Nina", "BRL"));
        bonusRuleRepository.save(new BonusRule(
                account,
                new BigDecimal("10.00"),
                BonusConditionType.NO_WITHDRAWALS_IN_MONTH,
                BonusBaseType.MONTHLY_DEPOSITS,
                true
        ));

        accountTransactionRepository.save(new AccountTransaction(
                account,
                TransactionType.DEPOSIT,
                TransactionOrigin.MANUAL,
                new BigDecimal("200.00"),
                "Mesada fevereiro",
                null,
                Instant.parse("2026-02-10T10:00:00Z")
        ));

        BonusExecutionSummary summary = bonusExecutionService.executeForReferenceMonth(YearMonth.of(2026, 2));

        assertThat(summary.totalRules()).isEqualTo(1);
        assertThat(summary.appliedBonuses()).isEqualTo(1);
        assertThat(accountTransactionRepository.existsByAccount_IdAndOriginAndEvidenceReference(
                account.getId(),
                TransactionOrigin.BONUS,
                "bonus:2026-02"
        )).isTrue();

        BigDecimal updatedBalance = accountTransactionRepository.calculateBalanceByAccountId(account.getId());
        assertThat(updatedBalance).isEqualByComparingTo("220.00");

        List<AuditEvent> events = auditEventRepository.findByAccountIdOrderByCreatedAtAsc(account.getId());
        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getActionType()).isEqualTo(AuditActionType.BONUS_APPLIED);
        assertThat(events.getFirst().getActorEmail()).isEqualTo("system.bonus@test.local");
    }
}

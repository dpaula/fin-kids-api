package br.com.autevia.finkidsapi.repository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.autevia.finkidsapi.domain.entity.Account;
import br.com.autevia.finkidsapi.domain.entity.AccountTransaction;
import br.com.autevia.finkidsapi.domain.entity.BonusRule;
import br.com.autevia.finkidsapi.domain.entity.Goal;
import br.com.autevia.finkidsapi.domain.enums.BonusBaseType;
import br.com.autevia.finkidsapi.domain.enums.BonusConditionType;
import br.com.autevia.finkidsapi.domain.enums.TransactionOrigin;
import br.com.autevia.finkidsapi.domain.enums.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DatabaseConstraintsIntegrationTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountTransactionRepository accountTransactionRepository;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private BonusRuleRepository bonusRuleRepository;

    @Test
    void shouldRejectTransactionWithNonPositiveAmount() {
        Account account = accountRepository.save(new Account("Constraint Child", "BRL"));
        AccountTransaction invalid = new AccountTransaction(
                account,
                TransactionType.DEPOSIT,
                TransactionOrigin.MANUAL,
                BigDecimal.ZERO,
                "Valor invalido",
                null,
                Instant.parse("2026-03-01T10:00:00Z")
        );

        assertThatThrownBy(() -> accountTransactionRepository.saveAndFlush(invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectGoalWithNonPositiveTargetAmount() {
        Account account = accountRepository.save(new Account("Constraint Goal", "BRL"));
        Goal invalid = new Goal(account, "Meta invalida", BigDecimal.ZERO, true);

        assertThatThrownBy(() -> goalRepository.saveAndFlush(invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectBonusRuleWithPercentageOutOfRange() {
        Account account = accountRepository.save(new Account("Constraint Bonus", "BRL"));
        BonusRule invalid = new BonusRule(
                account,
                new BigDecimal("120.00"),
                BonusConditionType.NO_WITHDRAWALS_IN_MONTH,
                BonusBaseType.LAST_BALANCE,
                true
        );

        assertThatThrownBy(() -> bonusRuleRepository.saveAndFlush(invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}

package br.com.autevia.finkidsapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.autevia.finkidsapi.domain.entity.Account;
import br.com.autevia.finkidsapi.domain.entity.AccountTransaction;
import br.com.autevia.finkidsapi.domain.entity.BonusRule;
import br.com.autevia.finkidsapi.domain.entity.Goal;
import br.com.autevia.finkidsapi.domain.enums.BonusBaseType;
import br.com.autevia.finkidsapi.domain.enums.BonusConditionType;
import br.com.autevia.finkidsapi.domain.enums.TransactionOrigin;
import br.com.autevia.finkidsapi.domain.enums.TransactionType;
import br.com.autevia.finkidsapi.domain.exception.ValidationException;
import br.com.autevia.finkidsapi.repository.AccountRepository;
import br.com.autevia.finkidsapi.repository.AccountTransactionRepository;
import br.com.autevia.finkidsapi.repository.BonusRuleRepository;
import br.com.autevia.finkidsapi.repository.GoalRepository;
import br.com.autevia.finkidsapi.service.dto.account.MonthlySummaryByOriginResult;
import br.com.autevia.finkidsapi.service.dto.account.MonthlySummaryByTypeResult;
import br.com.autevia.finkidsapi.service.dto.account.MonthlySummaryResult;
import br.com.autevia.finkidsapi.service.dto.view.ChildAccountViewResult;
import br.com.autevia.finkidsapi.service.dto.view.ParentAccountViewResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AccountViewServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountTransactionRepository accountTransactionRepository;

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private BonusRuleRepository bonusRuleRepository;

    @Mock
    private AccountSummaryService accountSummaryService;

    private AccountViewService accountViewService;

    @BeforeEach
    void setUp() {
        accountViewService = new AccountViewService(
                accountRepository,
                accountTransactionRepository,
                goalRepository,
                bonusRuleRepository,
                accountSummaryService
        );
    }

    @Test
    void shouldBuildChildViewWithGoalProgressAndRecentTransactions() {
        Account account = account(1L, "Lucas");
        Goal bike = goal(11L, account, "Bicicleta", "500.00");
        Goal game = goal(12L, account, "Video Game", "150.00");
        AccountTransaction deposit = transaction(
                101L,
                account,
                TransactionType.DEPOSIT,
                TransactionOrigin.MANUAL,
                "100.00",
                "Mesada",
                null,
                "2026-03-01T10:00:00Z"
        );

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountTransactionRepository.calculateBalanceByAccountId(1L)).thenReturn(new BigDecimal("180.00"));
        when(goalRepository.findByAccountIdAndActiveTrue(1L)).thenReturn(List.of(bike, game));
        when(accountTransactionRepository.findByAccountIdOrderByOccurredAtDesc(
                org.mockito.ArgumentMatchers.eq(1L),
                any(org.springframework.data.domain.Pageable.class)
        )).thenReturn(List.of(deposit));

        ChildAccountViewResult result = accountViewService.getChildView(1L, 10);

        assertThat(result.accountId()).isEqualTo(1L);
        assertThat(result.currentBalance()).isEqualByComparingTo("180.00");
        assertThat(result.goals()).hasSize(2);
        assertThat(result.goals().getFirst().progressAmount()).isEqualByComparingTo("180.00");
        assertThat(result.goals().getFirst().progressPercent()).isEqualByComparingTo("36.00");
        assertThat(result.goals().getFirst().remainingAmount()).isEqualByComparingTo("320.00");
        assertThat(result.goals().getFirst().achieved()).isFalse();
        assertThat(result.goals().get(1).progressAmount()).isEqualByComparingTo("150.00");
        assertThat(result.goals().get(1).progressPercent()).isEqualByComparingTo("100.00");
        assertThat(result.goals().get(1).remainingAmount()).isEqualByComparingTo("0.00");
        assertThat(result.goals().get(1).achieved()).isTrue();
        assertThat(result.recentTransactions()).hasSize(1);
        assertThat(result.recentTransactions().getFirst().type()).isEqualTo(TransactionType.DEPOSIT);
    }

    @Test
    void shouldBuildParentViewWithMonthlySummaryAndBonusRule() {
        Account account = account(1L, "Ana");
        Goal goal = goal(11L, account, "Patins", "250.00");
        AccountTransaction withdraw = transaction(
                300L,
                account,
                TransactionType.WITHDRAW,
                TransactionOrigin.WHATSAPP,
                "70.00",
                "Compra",
                "wa-1",
                "2026-03-08T11:00:00Z"
        );
        BonusRule bonusRule = bonusRule(account, "5.00");
        MonthlySummaryResult monthlySummary = new MonthlySummaryResult(
                1L,
                2026,
                3,
                Instant.parse("2026-03-01T00:00:00Z"),
                Instant.parse("2026-04-01T00:00:00Z"),
                new BigDecimal("180.00"),
                new BigDecimal("250.00"),
                new BigDecimal("70.00"),
                new BigDecimal("180.00"),
                List.of(
                        new MonthlySummaryByTypeResult(TransactionType.DEPOSIT, new BigDecimal("250.00")),
                        new MonthlySummaryByTypeResult(TransactionType.WITHDRAW, new BigDecimal("70.00"))
                ),
                List.of(
                        new MonthlySummaryByOriginResult(TransactionOrigin.MANUAL, new BigDecimal("180.00")),
                        new MonthlySummaryByOriginResult(TransactionOrigin.WHATSAPP, new BigDecimal("70.00"))
                )
        );

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountTransactionRepository.calculateBalanceByAccountId(1L)).thenReturn(new BigDecimal("180.00"));
        when(goalRepository.findByAccountIdAndActiveTrue(1L)).thenReturn(List.of(goal));
        when(accountSummaryService.getMonthlySummary(1L, 2026, 3)).thenReturn(monthlySummary);
        when(bonusRuleRepository.findByAccountId(1L)).thenReturn(Optional.of(bonusRule));
        when(accountTransactionRepository.findByAccountIdOrderByOccurredAtDesc(
                org.mockito.ArgumentMatchers.eq(1L),
                any(org.springframework.data.domain.Pageable.class)
        )).thenReturn(List.of(withdraw));

        ParentAccountViewResult result = accountViewService.getParentView(1L, 2026, 3, 20);

        assertThat(result.accountId()).isEqualTo(1L);
        assertThat(result.monthlySummary().year()).isEqualTo(2026);
        assertThat(result.monthlySummary().totalDeposits()).isEqualByComparingTo("250.00");
        assertThat(result.bonusRule()).isNotNull();
        assertThat(result.bonusRule().percentage()).isEqualByComparingTo("5.00");
        assertThat(result.recentTransactions()).hasSize(1);
        assertThat(result.recentTransactions().getFirst().origin()).isEqualTo(TransactionOrigin.WHATSAPP);
    }

    @Test
    void shouldReturnParentViewWithNullBonusRuleWhenNotConfigured() {
        Account account = account(1L, "Mia");
        MonthlySummaryResult monthlySummary = new MonthlySummaryResult(
                1L,
                2026,
                3,
                Instant.parse("2026-03-01T00:00:00Z"),
                Instant.parse("2026-04-01T00:00:00Z"),
                new BigDecimal("100.00"),
                new BigDecimal("100.00"),
                new BigDecimal("0.00"),
                new BigDecimal("100.00"),
                List.of(
                        new MonthlySummaryByTypeResult(TransactionType.DEPOSIT, new BigDecimal("100.00")),
                        new MonthlySummaryByTypeResult(TransactionType.WITHDRAW, new BigDecimal("0.00"))
                ),
                List.of(
                        new MonthlySummaryByOriginResult(TransactionOrigin.MANUAL, new BigDecimal("100.00"))
                )
        );

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountTransactionRepository.calculateBalanceByAccountId(1L)).thenReturn(new BigDecimal("100.00"));
        when(goalRepository.findByAccountIdAndActiveTrue(1L)).thenReturn(List.of());
        when(accountSummaryService.getMonthlySummary(1L, 2026, 3)).thenReturn(monthlySummary);
        when(bonusRuleRepository.findByAccountId(1L)).thenReturn(Optional.empty());
        when(accountTransactionRepository.findByAccountIdOrderByOccurredAtDesc(
                org.mockito.ArgumentMatchers.eq(1L),
                any(org.springframework.data.domain.Pageable.class)
        )).thenReturn(List.of());

        ParentAccountViewResult result = accountViewService.getParentView(1L, 2026, 3, 5);

        assertThat(result.bonusRule()).isNull();
        assertThat(result.goals()).isEmpty();
        assertThat(result.recentTransactions()).isEmpty();
    }

    @Test
    void shouldRejectInvalidRecentTransactionsLimit() {
        assertThatThrownBy(() -> accountViewService.getChildView(1L, 0))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("recentTransactionsLimit");
    }

    @Test
    void shouldRejectInvalidAccountId() {
        assertThatThrownBy(() -> accountViewService.getChildView(0L, 5))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("accountId");
    }

    private Account account(Long id, String childName) {
        Account account = new Account(childName, "BRL");
        ReflectionTestUtils.setField(account, "id", id);
        return account;
    }

    private Goal goal(Long id, Account account, String name, String targetAmount) {
        Goal goal = new Goal(account, name, new BigDecimal(targetAmount), true);
        ReflectionTestUtils.setField(goal, "id", id);
        return goal;
    }

    private AccountTransaction transaction(
            Long id,
            Account account,
            TransactionType type,
            TransactionOrigin origin,
            String amount,
            String description,
            String evidenceReference,
            String occurredAt
    ) {
        AccountTransaction transaction = new AccountTransaction(
                account,
                type,
                origin,
                new BigDecimal(amount),
                description,
                evidenceReference,
                Instant.parse(occurredAt)
        );
        ReflectionTestUtils.setField(transaction, "id", id);
        return transaction;
    }

    private BonusRule bonusRule(Account account, String percentage) {
        BonusRule rule = new BonusRule(
                account,
                new BigDecimal(percentage),
                BonusConditionType.NO_WITHDRAWALS_IN_MONTH,
                BonusBaseType.MONTHLY_DEPOSITS,
                true
        );
        ReflectionTestUtils.setField(rule, "id", 1L);
        return rule;
    }
}

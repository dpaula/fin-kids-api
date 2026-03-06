package br.com.autevia.finkidsapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.autevia.finkidsapi.domain.entity.Account;
import br.com.autevia.finkidsapi.domain.entity.AccountTransaction;
import br.com.autevia.finkidsapi.domain.entity.BonusRule;
import br.com.autevia.finkidsapi.domain.enums.AuditActionType;
import br.com.autevia.finkidsapi.domain.enums.BonusBaseType;
import br.com.autevia.finkidsapi.domain.enums.BonusConditionType;
import br.com.autevia.finkidsapi.domain.enums.TransactionOrigin;
import br.com.autevia.finkidsapi.domain.enums.TransactionType;
import br.com.autevia.finkidsapi.repository.AccountTransactionRepository;
import br.com.autevia.finkidsapi.repository.BonusRuleRepository;
import br.com.autevia.finkidsapi.service.dto.CreateTransactionCommand;
import br.com.autevia.finkidsapi.service.dto.CreateTransactionResult;
import br.com.autevia.finkidsapi.service.dto.audit.AuditRecordCommand;
import br.com.autevia.finkidsapi.service.dto.bonus.BonusExecutionSummary;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BonusExecutionServiceTest {

    @Mock
    private BonusRuleRepository bonusRuleRepository;

    @Mock
    private AccountTransactionRepository accountTransactionRepository;

    @Mock
    private TransactionService transactionService;

    @Mock
    private AuditTrailService auditTrailService;

    private BonusExecutionService bonusExecutionService;

    @BeforeEach
    void setUp() {
        bonusExecutionService = new BonusExecutionService(
                bonusRuleRepository,
                accountTransactionRepository,
                transactionService,
                auditTrailService,
                "system.bonus@test.local"
        );
    }

    @Test
    void shouldApplyMonthlyDepositsBonusWhenEligible() {
        BonusRule rule = activeRule(1L, "10.00", BonusBaseType.MONTHLY_DEPOSITS);
        when(bonusRuleRepository.findByActiveTrue()).thenReturn(List.of(rule));
        when(accountTransactionRepository.existsByAccount_IdAndOriginAndEvidenceReference(
                1L, TransactionOrigin.BONUS, "bonus:2026-02"
        )).thenReturn(false);
        when(accountTransactionRepository.existsByAccountIdAndTypeAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
                1L, TransactionType.WITHDRAW, Instant.parse("2026-02-01T00:00:00Z"), Instant.parse("2026-03-01T00:00:00Z")
        )).thenReturn(false);
        when(accountTransactionRepository.sumAmountByAccountIdAndTypeAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
                1L, TransactionType.DEPOSIT, Instant.parse("2026-02-01T00:00:00Z"), Instant.parse("2026-03-01T00:00:00Z")
        )).thenReturn(new BigDecimal("200.00"));
        when(transactionService.createTransaction(any(CreateTransactionCommand.class)))
                .thenReturn(new CreateTransactionResult(99L, new BigDecimal("220.00")));

        BonusExecutionSummary summary = bonusExecutionService.executeForReferenceMonth(YearMonth.of(2026, 2));

        assertThat(summary.totalRules()).isEqualTo(1);
        assertThat(summary.eligibleRules()).isEqualTo(1);
        assertThat(summary.appliedBonuses()).isEqualTo(1);
        assertThat(summary.skippedRules()).isEqualTo(0);
        assertThat(summary.failedRules()).isEqualTo(0);

        ArgumentCaptor<CreateTransactionCommand> txCaptor = ArgumentCaptor.forClass(CreateTransactionCommand.class);
        verify(transactionService).createTransaction(txCaptor.capture());
        CreateTransactionCommand command = txCaptor.getValue();
        assertThat(command.accountId()).isEqualTo(1L);
        assertThat(command.type()).isEqualTo(TransactionType.DEPOSIT);
        assertThat(command.origin()).isEqualTo(TransactionOrigin.BONUS);
        assertThat(command.amount()).isEqualByComparingTo("20.00");
        assertThat(command.evidenceReference()).isEqualTo("bonus:2026-02");
        assertThat(command.occurredAt()).isEqualTo(Instant.parse("2026-03-01T00:00:00Z"));

        ArgumentCaptor<AuditRecordCommand> auditCaptor = ArgumentCaptor.forClass(AuditRecordCommand.class);
        verify(auditTrailService).recordSystem(auditCaptor.capture(), org.mockito.ArgumentMatchers.eq("system.bonus@test.local"));
        assertThat(auditCaptor.getValue().actionType()).isEqualTo(AuditActionType.BONUS_APPLIED);
    }

    @Test
    void shouldSkipWhenAccountHasWithdrawalsInReferenceMonth() {
        BonusRule rule = activeRule(2L, "8.00", BonusBaseType.LAST_BALANCE);
        when(bonusRuleRepository.findByActiveTrue()).thenReturn(List.of(rule));
        when(accountTransactionRepository.existsByAccount_IdAndOriginAndEvidenceReference(
                2L, TransactionOrigin.BONUS, "bonus:2026-02"
        )).thenReturn(false);
        when(accountTransactionRepository.existsByAccountIdAndTypeAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
                2L, TransactionType.WITHDRAW, Instant.parse("2026-02-01T00:00:00Z"), Instant.parse("2026-03-01T00:00:00Z")
        )).thenReturn(true);

        BonusExecutionSummary summary = bonusExecutionService.executeForReferenceMonth(YearMonth.of(2026, 2));

        assertThat(summary.appliedBonuses()).isZero();
        assertThat(summary.skippedRules()).isEqualTo(1);
        verify(transactionService, never()).createTransaction(any());
        verify(auditTrailService, never()).recordSystem(any(), any());
    }

    @Test
    void shouldSkipWhenBonusAlreadyAppliedForMonth() {
        BonusRule rule = activeRule(3L, "5.00", BonusBaseType.LAST_ALLOWANCE);
        when(bonusRuleRepository.findByActiveTrue()).thenReturn(List.of(rule));
        when(accountTransactionRepository.existsByAccount_IdAndOriginAndEvidenceReference(
                3L, TransactionOrigin.BONUS, "bonus:2026-02"
        )).thenReturn(true);

        BonusExecutionSummary summary = bonusExecutionService.executeForReferenceMonth(YearMonth.of(2026, 2));

        assertThat(summary.appliedBonuses()).isZero();
        assertThat(summary.skippedRules()).isEqualTo(1);
        verify(transactionService, never()).createTransaction(any());
        verify(auditTrailService, never()).recordSystem(any(), any());
    }

    @Test
    void shouldContinueProcessingWhenSingleRuleFails() {
        BonusRule failingRule = activeRule(4L, "10.00", BonusBaseType.LAST_BALANCE);
        BonusRule okRule = activeRule(5L, "10.00", BonusBaseType.LAST_BALANCE);
        when(bonusRuleRepository.findByActiveTrue()).thenReturn(List.of(failingRule, okRule));

        when(accountTransactionRepository.existsByAccount_IdAndOriginAndEvidenceReference(4L, TransactionOrigin.BONUS, "bonus:2026-02"))
                .thenReturn(false);
        when(accountTransactionRepository.existsByAccountIdAndTypeAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
                4L, TransactionType.WITHDRAW, Instant.parse("2026-02-01T00:00:00Z"), Instant.parse("2026-03-01T00:00:00Z")
        )).thenReturn(false);
        when(accountTransactionRepository.calculateBalanceByAccountIdBefore(4L, Instant.parse("2026-03-01T00:00:00Z")))
                .thenReturn(new BigDecimal("150.00"));
        when(transactionService.createTransaction(any(CreateTransactionCommand.class)))
                .thenThrow(new RuntimeException("simulated failure"))
                .thenReturn(new CreateTransactionResult(123L, new BigDecimal("220.00")));

        when(accountTransactionRepository.existsByAccount_IdAndOriginAndEvidenceReference(5L, TransactionOrigin.BONUS, "bonus:2026-02"))
                .thenReturn(false);
        when(accountTransactionRepository.existsByAccountIdAndTypeAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
                5L, TransactionType.WITHDRAW, Instant.parse("2026-02-01T00:00:00Z"), Instant.parse("2026-03-01T00:00:00Z")
        )).thenReturn(false);
        when(accountTransactionRepository.calculateBalanceByAccountIdBefore(5L, Instant.parse("2026-03-01T00:00:00Z")))
                .thenReturn(new BigDecimal("200.00"));

        BonusExecutionSummary summary = bonusExecutionService.executeForReferenceMonth(YearMonth.of(2026, 2));

        assertThat(summary.totalRules()).isEqualTo(2);
        assertThat(summary.failedRules()).isEqualTo(1);
        assertThat(summary.appliedBonuses()).isEqualTo(1);
    }

    private BonusRule activeRule(Long accountId, String percentage, BonusBaseType baseType) {
        Account account = new Account("Kid " + accountId, "BRL");
        ReflectionTestUtils.setField(account, "id", accountId);
        BonusRule rule = new BonusRule(
                account,
                new BigDecimal(percentage),
                BonusConditionType.NO_WITHDRAWALS_IN_MONTH,
                baseType,
                true
        );
        ReflectionTestUtils.setField(rule, "id", accountId + 100);
        return rule;
    }
}

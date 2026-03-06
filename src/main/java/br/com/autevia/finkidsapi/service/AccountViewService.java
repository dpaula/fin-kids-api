package br.com.autevia.finkidsapi.service;

import br.com.autevia.finkidsapi.domain.entity.Account;
import br.com.autevia.finkidsapi.domain.entity.AccountTransaction;
import br.com.autevia.finkidsapi.domain.entity.BonusRule;
import br.com.autevia.finkidsapi.domain.entity.Goal;
import br.com.autevia.finkidsapi.domain.exception.ResourceNotFoundException;
import br.com.autevia.finkidsapi.domain.exception.ValidationException;
import br.com.autevia.finkidsapi.repository.AccountRepository;
import br.com.autevia.finkidsapi.repository.AccountTransactionRepository;
import br.com.autevia.finkidsapi.repository.BonusRuleRepository;
import br.com.autevia.finkidsapi.repository.GoalRepository;
import br.com.autevia.finkidsapi.service.dto.account.MonthlySummaryResult;
import br.com.autevia.finkidsapi.service.dto.view.ChildAccountViewResult;
import br.com.autevia.finkidsapi.service.dto.view.ChildTransactionItemResult;
import br.com.autevia.finkidsapi.service.dto.view.GoalProgressItemResult;
import br.com.autevia.finkidsapi.service.dto.view.ParentAccountViewResult;
import br.com.autevia.finkidsapi.service.dto.view.ParentBonusRuleResult;
import br.com.autevia.finkidsapi.service.dto.view.ParentMonthlySummaryByOriginResult;
import br.com.autevia.finkidsapi.service.dto.view.ParentMonthlySummaryByTypeResult;
import br.com.autevia.finkidsapi.service.dto.view.ParentMonthlySummaryResult;
import br.com.autevia.finkidsapi.service.dto.view.ParentTransactionItemResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountViewService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final int MIN_RECENT_TRANSACTIONS_LIMIT = 1;
    private static final int MAX_RECENT_TRANSACTIONS_LIMIT = 50;

    private final AccountRepository accountRepository;
    private final AccountTransactionRepository accountTransactionRepository;
    private final GoalRepository goalRepository;
    private final BonusRuleRepository bonusRuleRepository;
    private final AccountSummaryService accountSummaryService;

    public AccountViewService(
            AccountRepository accountRepository,
            AccountTransactionRepository accountTransactionRepository,
            GoalRepository goalRepository,
            BonusRuleRepository bonusRuleRepository,
            AccountSummaryService accountSummaryService
    ) {
        this.accountRepository = accountRepository;
        this.accountTransactionRepository = accountTransactionRepository;
        this.goalRepository = goalRepository;
        this.bonusRuleRepository = bonusRuleRepository;
        this.accountSummaryService = accountSummaryService;
    }

    @Transactional(readOnly = true)
    public ChildAccountViewResult getChildView(Long accountId, int recentTransactionsLimit) {
        validateAccountId(accountId);
        validateRecentTransactionsLimit(recentTransactionsLimit);

        Account account = findAccountOrThrow(accountId);
        BigDecimal currentBalance = getCurrentBalance(accountId);

        List<GoalProgressItemResult> goals = buildGoalProgressItems(accountId, currentBalance);
        List<ChildTransactionItemResult> recentTransactions = accountTransactionRepository
                .findByAccountIdOrderByOccurredAtDesc(accountId, PageRequest.of(0, recentTransactionsLimit))
                .stream()
                .map(this::toChildTransactionItem)
                .toList();

        return new ChildAccountViewResult(
                account.getId(),
                account.getChildName(),
                account.getCurrencyCode(),
                currentBalance,
                goals,
                recentTransactions
        );
    }

    @Transactional(readOnly = true)
    public ParentAccountViewResult getParentView(
            Long accountId,
            int year,
            int month,
            int recentTransactionsLimit
    ) {
        validateAccountId(accountId);
        validateRecentTransactionsLimit(recentTransactionsLimit);

        Account account = findAccountOrThrow(accountId);
        BigDecimal currentBalance = getCurrentBalance(accountId);
        List<GoalProgressItemResult> goals = buildGoalProgressItems(accountId, currentBalance);
        ParentMonthlySummaryResult monthlySummary = toParentMonthlySummary(
                accountSummaryService.getMonthlySummary(accountId, year, month)
        );

        ParentBonusRuleResult bonusRule = bonusRuleRepository.findByAccountId(accountId)
                .map(this::toParentBonusRule)
                .orElse(null);

        List<ParentTransactionItemResult> recentTransactions = accountTransactionRepository
                .findByAccountIdOrderByOccurredAtDesc(accountId, PageRequest.of(0, recentTransactionsLimit))
                .stream()
                .map(this::toParentTransactionItem)
                .toList();

        return new ParentAccountViewResult(
                account.getId(),
                account.getChildName(),
                account.getCurrencyCode(),
                currentBalance,
                monthlySummary,
                bonusRule,
                goals,
                recentTransactions
        );
    }

    private List<GoalProgressItemResult> buildGoalProgressItems(Long accountId, BigDecimal currentBalance) {
        return goalRepository.findByAccountIdAndActiveTrue(accountId).stream()
                .map(goal -> toGoalProgress(goal, currentBalance))
                .toList();
    }

    private GoalProgressItemResult toGoalProgress(Goal goal, BigDecimal currentBalance) {
        BigDecimal targetAmount = safeNumber(goal.getTargetAmount());
        BigDecimal normalizedBalance = safeNumber(currentBalance).max(ZERO);
        BigDecimal progressAmount = normalizedBalance.min(targetAmount);
        BigDecimal remainingAmount = targetAmount.subtract(progressAmount).max(ZERO);
        BigDecimal progressPercent = calculateProgressPercent(progressAmount, targetAmount);
        boolean achieved = remainingAmount.compareTo(ZERO) == 0;

        return new GoalProgressItemResult(
                goal.getId(),
                goal.getName(),
                targetAmount,
                progressAmount,
                progressPercent,
                remainingAmount,
                achieved
        );
    }

    private BigDecimal calculateProgressPercent(BigDecimal progressAmount, BigDecimal targetAmount) {
        if (targetAmount == null || targetAmount.compareTo(ZERO) <= 0) {
            return ZERO;
        }
        return progressAmount
                .multiply(HUNDRED)
                .divide(targetAmount, 2, RoundingMode.HALF_UP)
                .min(HUNDRED);
    }

    private ParentMonthlySummaryResult toParentMonthlySummary(MonthlySummaryResult monthlySummary) {
        return new ParentMonthlySummaryResult(
                monthlySummary.year(),
                monthlySummary.month(),
                monthlySummary.periodStart(),
                monthlySummary.periodEnd(),
                monthlySummary.totalDeposits(),
                monthlySummary.totalWithdrawals(),
                monthlySummary.netChange(),
                monthlySummary.totalsByType().stream()
                        .map(item -> new ParentMonthlySummaryByTypeResult(item.type(), item.total()))
                        .toList(),
                monthlySummary.totalsByOrigin().stream()
                        .map(item -> new ParentMonthlySummaryByOriginResult(item.origin(), item.total()))
                        .toList()
        );
    }

    private ParentBonusRuleResult toParentBonusRule(BonusRule rule) {
        return new ParentBonusRuleResult(
                rule.getPercentage(),
                rule.getConditionType(),
                rule.getBaseType(),
                rule.isActive()
        );
    }

    private ChildTransactionItemResult toChildTransactionItem(AccountTransaction transaction) {
        return new ChildTransactionItemResult(
                transaction.getId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getOccurredAt()
        );
    }

    private ParentTransactionItemResult toParentTransactionItem(AccountTransaction transaction) {
        return new ParentTransactionItemResult(
                transaction.getId(),
                transaction.getType(),
                transaction.getOrigin(),
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getEvidenceReference(),
                transaction.getOccurredAt()
        );
    }

    private Account findAccountOrThrow(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta nao encontrada para id=" + accountId));
    }

    private BigDecimal getCurrentBalance(Long accountId) {
        return safeNumber(accountTransactionRepository.calculateBalanceByAccountId(accountId));
    }

    private void validateAccountId(Long accountId) {
        if (accountId == null || accountId <= 0) {
            throw new ValidationException("accountId deve ser informado e maior que zero.");
        }
    }

    private void validateRecentTransactionsLimit(int recentTransactionsLimit) {
        if (recentTransactionsLimit < MIN_RECENT_TRANSACTIONS_LIMIT
                || recentTransactionsLimit > MAX_RECENT_TRANSACTIONS_LIMIT) {
            throw new ValidationException("recentTransactionsLimit deve estar entre 1 e 50.");
        }
    }

    private BigDecimal safeNumber(BigDecimal value) {
        return value == null ? ZERO : value;
    }
}

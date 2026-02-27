package br.com.autevia.finkidsapi.service;

import br.com.autevia.finkidsapi.domain.entity.Account;
import br.com.autevia.finkidsapi.domain.enums.TransactionOrigin;
import br.com.autevia.finkidsapi.domain.enums.TransactionType;
import br.com.autevia.finkidsapi.domain.exception.ResourceNotFoundException;
import br.com.autevia.finkidsapi.domain.exception.ValidationException;
import br.com.autevia.finkidsapi.repository.AccountRepository;
import br.com.autevia.finkidsapi.repository.AccountTransactionRepository;
import br.com.autevia.finkidsapi.repository.projection.TransactionOriginTotalProjection;
import br.com.autevia.finkidsapi.repository.projection.TransactionTypeTotalProjection;
import br.com.autevia.finkidsapi.service.dto.account.AccountBalanceResult;
import br.com.autevia.finkidsapi.service.dto.account.MonthlySummaryByOriginResult;
import br.com.autevia.finkidsapi.service.dto.account.MonthlySummaryByTypeResult;
import br.com.autevia.finkidsapi.service.dto.account.MonthlySummaryResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountSummaryService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final AccountRepository accountRepository;
    private final AccountTransactionRepository accountTransactionRepository;

    public AccountSummaryService(
            AccountRepository accountRepository,
            AccountTransactionRepository accountTransactionRepository
    ) {
        this.accountRepository = accountRepository;
        this.accountTransactionRepository = accountTransactionRepository;
    }

    @Transactional(readOnly = true)
    public AccountBalanceResult getBalance(Long accountId) {
        validateAccountId(accountId);
        findAccountOrThrow(accountId);
        return new AccountBalanceResult(accountId, getCurrentBalance(accountId));
    }

    @Transactional(readOnly = true)
    public MonthlySummaryResult getMonthlySummary(Long accountId, int year, int month) {
        validateAccountId(accountId);
        validateYearAndMonth(year, month);
        findAccountOrThrow(accountId);

        YearMonth yearMonth = YearMonth.of(year, month);
        Instant periodStart = yearMonth.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant periodEnd = yearMonth.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        Map<TransactionType, BigDecimal> totalsByType = initializeTypeTotals();
        for (TransactionTypeTotalProjection projection : accountTransactionRepository
                .summarizeMonthlyByType(accountId, periodStart, periodEnd)) {
            totalsByType.put(projection.getType(), safeNumber(projection.getTotal()));
        }

        Map<TransactionOrigin, BigDecimal> totalsByOrigin = initializeOriginTotals();
        for (TransactionOriginTotalProjection projection : accountTransactionRepository
                .summarizeMonthlyByOrigin(accountId, periodStart, periodEnd)) {
            totalsByOrigin.put(projection.getOrigin(), safeNumber(projection.getTotal()));
        }

        BigDecimal totalDeposits = totalsByType.get(TransactionType.DEPOSIT);
        BigDecimal totalWithdrawals = totalsByType.get(TransactionType.WITHDRAW);

        return new MonthlySummaryResult(
                accountId,
                year,
                month,
                periodStart,
                periodEnd,
                getCurrentBalance(accountId),
                totalDeposits,
                totalWithdrawals,
                totalDeposits.subtract(totalWithdrawals),
                totalsByType.entrySet().stream()
                        .map(entry -> new MonthlySummaryByTypeResult(entry.getKey(), entry.getValue()))
                        .toList(),
                totalsByOrigin.entrySet().stream()
                        .map(entry -> new MonthlySummaryByOriginResult(entry.getKey(), entry.getValue()))
                        .toList()
        );
    }

    private void validateAccountId(Long accountId) {
        if (accountId == null || accountId <= 0) {
            throw new ValidationException("accountId deve ser informado e maior que zero.");
        }
    }

    private void validateYearAndMonth(int year, int month) {
        if (year < 2000 || year > 2100) {
            throw new ValidationException("year deve estar entre 2000 e 2100.");
        }
        if (month < 1 || month > 12) {
            throw new ValidationException("month deve estar entre 1 e 12.");
        }
    }

    private Account findAccountOrThrow(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta nao encontrada para id=" + accountId));
    }

    private BigDecimal getCurrentBalance(Long accountId) {
        BigDecimal balance = accountTransactionRepository.calculateBalanceByAccountId(accountId);
        return safeNumber(balance);
    }

    private BigDecimal safeNumber(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private Map<TransactionType, BigDecimal> initializeTypeTotals() {
        Map<TransactionType, BigDecimal> totals = new EnumMap<>(TransactionType.class);
        Arrays.stream(TransactionType.values()).forEach(type -> totals.put(type, ZERO));
        return totals;
    }

    private Map<TransactionOrigin, BigDecimal> initializeOriginTotals() {
        Map<TransactionOrigin, BigDecimal> totals = new EnumMap<>(TransactionOrigin.class);
        Arrays.stream(TransactionOrigin.values()).forEach(origin -> totals.put(origin, ZERO));
        return totals;
    }
}

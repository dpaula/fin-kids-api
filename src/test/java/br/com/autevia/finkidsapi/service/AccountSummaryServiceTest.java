package br.com.autevia.finkidsapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import br.com.autevia.finkidsapi.domain.entity.Account;
import br.com.autevia.finkidsapi.domain.enums.TransactionOrigin;
import br.com.autevia.finkidsapi.domain.enums.TransactionType;
import br.com.autevia.finkidsapi.domain.exception.ValidationException;
import br.com.autevia.finkidsapi.repository.AccountRepository;
import br.com.autevia.finkidsapi.repository.AccountTransactionRepository;
import br.com.autevia.finkidsapi.repository.projection.TransactionOriginTotalProjection;
import br.com.autevia.finkidsapi.repository.projection.TransactionTypeTotalProjection;
import br.com.autevia.finkidsapi.service.dto.account.AccountBalanceResult;
import br.com.autevia.finkidsapi.service.dto.account.MonthlySummaryResult;
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
class AccountSummaryServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountTransactionRepository accountTransactionRepository;

    private AccountSummaryService accountSummaryService;

    @BeforeEach
    void setUp() {
        accountSummaryService = new AccountSummaryService(accountRepository, accountTransactionRepository);
    }

    @Test
    void shouldReturnCurrentBalance() {
        Account account = new Account("Lucas", "BRL");
        ReflectionTestUtils.setField(account, "id", 1L);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountTransactionRepository.calculateBalanceByAccountId(1L)).thenReturn(new BigDecimal("123.45"));

        AccountBalanceResult result = accountSummaryService.getBalance(1L);

        assertThat(result.accountId()).isEqualTo(1L);
        assertThat(result.currentBalance()).isEqualByComparingTo("123.45");
    }

    @Test
    void shouldBuildMonthlySummaryWithTotalsByTypeAndOrigin() {
        Account account = new Account("Lucas", "BRL");
        ReflectionTestUtils.setField(account, "id", 1L);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountTransactionRepository.calculateBalanceByAccountId(1L)).thenReturn(new BigDecimal("180.00"));
        when(accountTransactionRepository.summarizeMonthlyByType(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.any(Instant.class),
                org.mockito.ArgumentMatchers.any(Instant.class)
        )).thenReturn(List.of(
                typeProjection(TransactionType.DEPOSIT, new BigDecimal("250.00")),
                typeProjection(TransactionType.WITHDRAW, new BigDecimal("70.00"))
        ));
        when(accountTransactionRepository.summarizeMonthlyByOrigin(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.any(Instant.class),
                org.mockito.ArgumentMatchers.any(Instant.class)
        )).thenReturn(List.of(
                originProjection(TransactionOrigin.MANUAL, new BigDecimal("200.00")),
                originProjection(TransactionOrigin.BONUS, new BigDecimal("50.00")),
                originProjection(TransactionOrigin.WHATSAPP, new BigDecimal("70.00"))
        ));

        MonthlySummaryResult result = accountSummaryService.getMonthlySummary(1L, 2026, 2);

        assertThat(result.totalDeposits()).isEqualByComparingTo("250.00");
        assertThat(result.totalWithdrawals()).isEqualByComparingTo("70.00");
        assertThat(result.netChange()).isEqualByComparingTo("180.00");
        assertThat(result.currentBalance()).isEqualByComparingTo("180.00");
        assertThat(result.totalsByType()).hasSize(2);
        assertThat(result.totalsByOrigin()).hasSize(3);
    }

    @Test
    void shouldReturnZeroWhenMonthHasNoTransactions() {
        Account account = new Account("Lucas", "BRL");
        ReflectionTestUtils.setField(account, "id", 1L);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountTransactionRepository.calculateBalanceByAccountId(1L)).thenReturn(null);
        when(accountTransactionRepository.summarizeMonthlyByType(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.any(Instant.class),
                org.mockito.ArgumentMatchers.any(Instant.class)
        )).thenReturn(List.of());
        when(accountTransactionRepository.summarizeMonthlyByOrigin(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.any(Instant.class),
                org.mockito.ArgumentMatchers.any(Instant.class)
        )).thenReturn(List.of());

        MonthlySummaryResult result = accountSummaryService.getMonthlySummary(1L, 2026, 3);

        assertThat(result.totalDeposits()).isEqualByComparingTo("0");
        assertThat(result.totalWithdrawals()).isEqualByComparingTo("0");
        assertThat(result.netChange()).isEqualByComparingTo("0");
        assertThat(result.currentBalance()).isEqualByComparingTo("0");
    }

    @Test
    void shouldRejectInvalidMonth() {
        assertThatThrownBy(() -> accountSummaryService.getMonthlySummary(1L, 2026, 13))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("month");
    }

    private TransactionTypeTotalProjection typeProjection(TransactionType type, BigDecimal total) {
        return new TransactionTypeTotalProjection() {
            @Override
            public TransactionType getType() {
                return type;
            }

            @Override
            public BigDecimal getTotal() {
                return total;
            }
        };
    }

    private TransactionOriginTotalProjection originProjection(TransactionOrigin origin, BigDecimal total) {
        return new TransactionOriginTotalProjection() {
            @Override
            public TransactionOrigin getOrigin() {
                return origin;
            }

            @Override
            public BigDecimal getTotal() {
                return total;
            }
        };
    }
}

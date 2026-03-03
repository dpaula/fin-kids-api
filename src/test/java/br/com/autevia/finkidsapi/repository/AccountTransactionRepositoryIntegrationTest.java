package br.com.autevia.finkidsapi.repository;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.autevia.finkidsapi.domain.entity.Account;
import br.com.autevia.finkidsapi.domain.entity.AccountTransaction;
import br.com.autevia.finkidsapi.domain.enums.TransactionOrigin;
import br.com.autevia.finkidsapi.domain.enums.TransactionType;
import br.com.autevia.finkidsapi.repository.projection.TransactionOriginTotalProjection;
import br.com.autevia.finkidsapi.repository.projection.TransactionTypeTotalProjection;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AccountTransactionRepositoryIntegrationTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountTransactionRepository accountTransactionRepository;

    @Test
    void shouldCalculateBalanceFromTransactionHistory() {
        Account account = accountRepository.save(new Account("Lucas", "BRL"));

        saveTransaction(account, TransactionType.DEPOSIT, TransactionOrigin.MANUAL, "100.00", "Mesada", "2026-02-01T10:00:00Z");
        saveTransaction(account, TransactionType.WITHDRAW, TransactionOrigin.MANUAL, "40.00", "Lanche", "2026-02-02T10:00:00Z");
        saveTransaction(account, TransactionType.DEPOSIT, TransactionOrigin.WHATSAPP, "20.00", "Pix", "2026-02-03T10:00:00Z");

        BigDecimal balance = accountTransactionRepository.calculateBalanceByAccountId(account.getId());

        assertThat(balance).isEqualByComparingTo("80.00");
    }

    @Test
    void shouldSummarizeMonthlyTotalsByTypeAndOrigin() {
        Account account = accountRepository.save(new Account("Ana", "BRL"));

        saveTransaction(account, TransactionType.DEPOSIT, TransactionOrigin.MANUAL, "100.00", "Mesada", "2026-02-05T10:00:00Z");
        saveTransaction(account, TransactionType.DEPOSIT, TransactionOrigin.WHATSAPP, "30.00", "Pix", "2026-02-07T10:00:00Z");
        saveTransaction(account, TransactionType.WITHDRAW, TransactionOrigin.MANUAL, "20.00", "Compra", "2026-02-10T10:00:00Z");
        saveTransaction(account, TransactionType.DEPOSIT, TransactionOrigin.BONUS, "15.00", "Bonus", "2026-03-01T01:00:00Z");

        Instant start = Instant.parse("2026-02-01T00:00:00Z");
        Instant end = Instant.parse("2026-03-01T00:00:00Z");

        Map<TransactionType, BigDecimal> byType = accountTransactionRepository
                .summarizeMonthlyByType(account.getId(), start, end)
                .stream()
                .collect(Collectors.toMap(TransactionTypeTotalProjection::getType, TransactionTypeTotalProjection::getTotal));

        Map<TransactionOrigin, BigDecimal> byOrigin = accountTransactionRepository
                .summarizeMonthlyByOrigin(account.getId(), start, end)
                .stream()
                .collect(Collectors.toMap(TransactionOriginTotalProjection::getOrigin, TransactionOriginTotalProjection::getTotal));

        assertThat(byType.get(TransactionType.DEPOSIT)).isEqualByComparingTo("130.00");
        assertThat(byType.get(TransactionType.WITHDRAW)).isEqualByComparingTo("20.00");

        assertThat(byOrigin.get(TransactionOrigin.MANUAL)).isEqualByComparingTo("120.00");
        assertThat(byOrigin.get(TransactionOrigin.WHATSAPP)).isEqualByComparingTo("30.00");
        assertThat(byOrigin.get(TransactionOrigin.BONUS)).isNull();
    }

    private void saveTransaction(
            Account account,
            TransactionType type,
            TransactionOrigin origin,
            String amount,
            String description,
            String occurredAt
    ) {
        AccountTransaction tx = new AccountTransaction(
                account,
                type,
                origin,
                new BigDecimal(amount),
                description,
                null,
                Instant.parse(occurredAt)
        );
        accountTransactionRepository.save(tx);
    }
}

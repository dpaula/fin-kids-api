package br.com.autevia.finkidsapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.autevia.finkidsapi.domain.entity.Account;
import br.com.autevia.finkidsapi.domain.entity.AccountTransaction;
import br.com.autevia.finkidsapi.domain.enums.TransactionOrigin;
import br.com.autevia.finkidsapi.domain.enums.TransactionType;
import br.com.autevia.finkidsapi.domain.exception.BusinessRuleException;
import br.com.autevia.finkidsapi.domain.exception.DuplicateTransactionException;
import br.com.autevia.finkidsapi.domain.exception.ValidationException;
import br.com.autevia.finkidsapi.repository.AccountRepository;
import br.com.autevia.finkidsapi.repository.AccountTransactionRepository;
import br.com.autevia.finkidsapi.service.dto.CreateTransactionCommand;
import br.com.autevia.finkidsapi.service.dto.CreateTransactionResult;
import br.com.autevia.finkidsapi.service.dto.TransactionListResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountTransactionRepository accountTransactionRepository;

    @Mock
    private AuditTrailService auditTrailService;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(accountRepository, accountTransactionRepository, auditTrailService);
    }

    @Test
    void shouldCreateDepositTransaction() {
        Account account = new Account("Lucas", "BRL");
        ReflectionTestUtils.setField(account, "id", 1L);

        CreateTransactionCommand command = new CreateTransactionCommand(
                1L,
                TransactionType.DEPOSIT,
                TransactionOrigin.MANUAL,
                new BigDecimal("100.00"),
                "Mesada",
                null,
                Instant.parse("2026-02-27T12:00:00Z")
        );

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountTransactionRepository.calculateBalanceByAccountId(1L)).thenReturn(new BigDecimal("50.00"));
        when(accountTransactionRepository.save(any(AccountTransaction.class))).thenAnswer(invocation -> {
            AccountTransaction tx = invocation.getArgument(0);
            ReflectionTestUtils.setField(tx, "id", 10L);
            return tx;
        });

        CreateTransactionResult result = transactionService.createTransaction(command);

        assertThat(result.transactionId()).isEqualTo(10L);
        assertThat(result.updatedBalance()).isEqualByComparingTo("150.00");
        verify(accountTransactionRepository).save(any(AccountTransaction.class));
        verify(auditTrailService).record(any());
    }

    @Test
    void shouldBlockWithdrawWhenInsufficientBalance() {
        Account account = new Account("Lucas", "BRL");
        ReflectionTestUtils.setField(account, "id", 1L);

        CreateTransactionCommand command = new CreateTransactionCommand(
                1L,
                TransactionType.WITHDRAW,
                TransactionOrigin.MANUAL,
                new BigDecimal("70.00"),
                "Compra",
                null,
                Instant.now()
        );

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountTransactionRepository.calculateBalanceByAccountId(1L)).thenReturn(new BigDecimal("30.00"));

        assertThatThrownBy(() -> transactionService.createTransaction(command))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Saldo insuficiente");

        verify(accountTransactionRepository, never()).save(any(AccountTransaction.class));
        verify(auditTrailService, never()).record(any());
    }

    @Test
    void shouldRejectDuplicateTransactionByEvidenceReference() {
        Account account = new Account("Lucas", "BRL");
        ReflectionTestUtils.setField(account, "id", 1L);

        CreateTransactionCommand command = new CreateTransactionCommand(
                1L,
                TransactionType.DEPOSIT,
                TransactionOrigin.WHATSAPP,
                new BigDecimal("70.00"),
                "Deposito lido",
                "wa-media-001",
                Instant.now()
        );

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountTransactionRepository.calculateBalanceByAccountId(1L)).thenReturn(new BigDecimal("30.00"));
        when(accountTransactionRepository.existsByAccount_IdAndOriginAndEvidenceReference(
                1L,
                TransactionOrigin.WHATSAPP,
                "wa-media-001"
        )).thenReturn(true);

        assertThatThrownBy(() -> transactionService.createTransaction(command))
                .isInstanceOf(DuplicateTransactionException.class)
                .hasMessageContaining("Transacao duplicada");

        verify(accountTransactionRepository, never()).save(any(AccountTransaction.class));
        verify(auditTrailService, never()).record(any());
    }

    @Test
    void shouldTranslateDatabaseUniqueViolationToDuplicateTransactionException() {
        Account account = new Account("Lucas", "BRL");
        ReflectionTestUtils.setField(account, "id", 1L);

        CreateTransactionCommand command = new CreateTransactionCommand(
                1L,
                TransactionType.DEPOSIT,
                TransactionOrigin.WHATSAPP,
                new BigDecimal("70.00"),
                "Deposito lido",
                "wa-media-002",
                Instant.now()
        );

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountTransactionRepository.calculateBalanceByAccountId(1L)).thenReturn(new BigDecimal("30.00"));
        when(accountTransactionRepository.existsByAccount_IdAndOriginAndEvidenceReference(
                1L,
                TransactionOrigin.WHATSAPP,
                "wa-media-002"
        )).thenReturn(false);
        when(accountTransactionRepository.save(any(AccountTransaction.class)))
                .thenThrow(new DataIntegrityViolationException("unique violation"));

        assertThatThrownBy(() -> transactionService.createTransaction(command))
                .isInstanceOf(DuplicateTransactionException.class)
                .hasMessageContaining("Transacao duplicada");
        verify(auditTrailService, never()).record(any());
    }

    @Test
    void shouldRejectInvalidAmount() {
        CreateTransactionCommand command = new CreateTransactionCommand(
                1L,
                TransactionType.DEPOSIT,
                TransactionOrigin.MANUAL,
                BigDecimal.ZERO,
                "Mesada",
                null,
                Instant.now()
        );

        assertThatThrownBy(() -> transactionService.createTransaction(command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("amount");
    }

    @Test
    void shouldListTransactionsByPeriod() {
        Account account = new Account("Lucas", "BRL");
        ReflectionTestUtils.setField(account, "id", 1L);

        AccountTransaction transaction = new AccountTransaction(
                account,
                TransactionType.DEPOSIT,
                TransactionOrigin.MANUAL,
                new BigDecimal("40.00"),
                "Mesada",
                null,
                Instant.parse("2026-02-20T10:00:00Z")
        );
        ReflectionTestUtils.setField(transaction, "id", 77L);

        Instant start = Instant.parse("2026-02-01T00:00:00Z");
        Instant end = Instant.parse("2026-02-28T23:59:59Z");

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountTransactionRepository.findByAccountIdAndOccurredAtBetweenOrderByOccurredAtDesc(1L, start, end))
                .thenReturn(List.of(transaction));
        when(accountTransactionRepository.calculateBalanceByAccountId(1L)).thenReturn(new BigDecimal("90.00"));

        TransactionListResult result = transactionService.listTransactions(1L, start, end);

        assertThat(result.currentBalance()).isEqualByComparingTo("90.00");
        assertThat(result.transactions()).hasSize(1);
        assertThat(result.transactions().getFirst().transactionId()).isEqualTo(77L);
    }
}

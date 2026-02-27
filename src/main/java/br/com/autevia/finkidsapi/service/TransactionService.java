package br.com.autevia.finkidsapi.service;

import br.com.autevia.finkidsapi.domain.entity.Account;
import br.com.autevia.finkidsapi.domain.entity.AccountTransaction;
import br.com.autevia.finkidsapi.domain.enums.TransactionOrigin;
import br.com.autevia.finkidsapi.domain.enums.TransactionType;
import br.com.autevia.finkidsapi.domain.exception.BusinessRuleException;
import br.com.autevia.finkidsapi.domain.exception.DuplicateTransactionException;
import br.com.autevia.finkidsapi.domain.exception.ResourceNotFoundException;
import br.com.autevia.finkidsapi.domain.exception.ValidationException;
import br.com.autevia.finkidsapi.repository.AccountRepository;
import br.com.autevia.finkidsapi.repository.AccountTransactionRepository;
import br.com.autevia.finkidsapi.service.dto.CreateTransactionCommand;
import br.com.autevia.finkidsapi.service.dto.CreateTransactionResult;
import br.com.autevia.finkidsapi.service.dto.TransactionItemResult;
import br.com.autevia.finkidsapi.service.dto.TransactionListResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final AccountRepository accountRepository;
    private final AccountTransactionRepository accountTransactionRepository;

    public TransactionService(
            AccountRepository accountRepository,
            AccountTransactionRepository accountTransactionRepository
    ) {
        this.accountRepository = accountRepository;
        this.accountTransactionRepository = accountTransactionRepository;
    }

    @Transactional
    public CreateTransactionResult createTransaction(CreateTransactionCommand command) {
        validateCreateCommand(command);

        Account account = findAccountOrThrow(command.accountId());
        BigDecimal currentBalance = getCurrentBalance(account.getId());
        String normalizedEvidence = normalizeEvidenceReference(command.evidenceReference());

        if (normalizedEvidence != null && isDuplicateEvidence(command.accountId(), command.origin(), normalizedEvidence)) {
            throw new DuplicateTransactionException(
                    "Transacao duplicada para a mesma evidencia informada."
            );
        }

        if (command.type() == TransactionType.WITHDRAW && currentBalance.compareTo(command.amount()) < 0) {
            throw new BusinessRuleException("Saldo insuficiente para realizar saque.");
        }

        AccountTransaction transaction = new AccountTransaction(
                account,
                command.type(),
                command.origin(),
                command.amount(),
                command.description().trim(),
                normalizedEvidence,
                command.occurredAt()
        );

        AccountTransaction saved;
        try {
            saved = accountTransactionRepository.save(transaction);
        } catch (DataIntegrityViolationException ex) {
            if (normalizedEvidence != null) {
                throw new DuplicateTransactionException("Transacao duplicada para a mesma evidencia informada.");
            }
            throw ex;
        }
        BigDecimal updatedBalance = applyAmount(currentBalance, command.type(), command.amount());

        return new CreateTransactionResult(saved.getId(), updatedBalance);
    }

    @Transactional(readOnly = true)
    public TransactionListResult listTransactions(Long accountId, Instant start, Instant end) {
        validateListParams(accountId, start, end);
        findAccountOrThrow(accountId);

        List<AccountTransaction> transactions = accountTransactionRepository
                .findByAccountIdAndOccurredAtBetweenOrderByOccurredAtDesc(accountId, start, end);

        List<TransactionItemResult> items = transactions.stream()
                .map(this::toItem)
                .toList();

        return new TransactionListResult(getCurrentBalance(accountId), items);
    }

    private void validateCreateCommand(CreateTransactionCommand command) {
        if (command == null) {
            throw new ValidationException("Comando de transacao e obrigatorio.");
        }
        if (command.accountId() == null || command.accountId() <= 0) {
            throw new ValidationException("accountId deve ser informado e maior que zero.");
        }
        if (command.type() == null) {
            throw new ValidationException("type e obrigatorio.");
        }
        if (command.origin() == null) {
            throw new ValidationException("origin e obrigatorio.");
        }
        if (command.amount() == null || command.amount().compareTo(ZERO) <= 0) {
            throw new ValidationException("amount deve ser maior que zero.");
        }
        if (command.description() == null || command.description().isBlank()) {
            throw new ValidationException("description e obrigatoria.");
        }
    }

    private void validateListParams(Long accountId, Instant start, Instant end) {
        if (accountId == null || accountId <= 0) {
            throw new ValidationException("accountId deve ser informado e maior que zero.");
        }
        if (start == null || end == null) {
            throw new ValidationException("start e end devem ser informados.");
        }
        if (start.isAfter(end)) {
            throw new ValidationException("start nao pode ser maior que end.");
        }
    }

    private Account findAccountOrThrow(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta nao encontrada para id=" + accountId));
    }

    private BigDecimal getCurrentBalance(Long accountId) {
        BigDecimal balance = accountTransactionRepository.calculateBalanceByAccountId(accountId);
        return balance == null ? ZERO : balance;
    }

    private BigDecimal applyAmount(BigDecimal currentBalance, TransactionType type, BigDecimal amount) {
        return type == TransactionType.DEPOSIT
                ? currentBalance.add(amount)
                : currentBalance.subtract(amount);
    }

    private String normalizeEvidenceReference(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private boolean isDuplicateEvidence(Long accountId, TransactionOrigin origin, String evidenceReference) {
        return accountTransactionRepository.existsByAccount_IdAndOriginAndEvidenceReference(
                accountId,
                origin,
                evidenceReference
        );
    }

    private TransactionItemResult toItem(AccountTransaction transaction) {
        return new TransactionItemResult(
                transaction.getId(),
                transaction.getAccount().getId(),
                transaction.getType(),
                transaction.getOrigin(),
                transaction.getAmount(),
                transaction.getDescription(),
                transaction.getEvidenceReference(),
                transaction.getOccurredAt()
        );
    }
}

package br.com.autevia.finkidsapi.service.dto.view;

import br.com.autevia.finkidsapi.domain.enums.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;

public record ChildTransactionItemResult(
        Long transactionId,
        TransactionType type,
        BigDecimal amount,
        String description,
        Instant occurredAt
) {
}

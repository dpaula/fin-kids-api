package br.com.autevia.finkidsapi.service.dto.view;

import br.com.autevia.finkidsapi.domain.enums.TransactionOrigin;
import br.com.autevia.finkidsapi.domain.enums.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;

public record ParentTransactionItemResult(
        Long transactionId,
        TransactionType type,
        TransactionOrigin origin,
        BigDecimal amount,
        String description,
        String evidenceReference,
        Instant occurredAt
) {
}

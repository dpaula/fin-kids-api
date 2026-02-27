package br.com.autevia.finkidsapi.service.dto;

import java.math.BigDecimal;

public record CreateTransactionResult(
        Long transactionId,
        BigDecimal updatedBalance
) {
}

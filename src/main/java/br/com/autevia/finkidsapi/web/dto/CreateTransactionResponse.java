package br.com.autevia.finkidsapi.web.dto;

import java.math.BigDecimal;

public record CreateTransactionResponse(
        Long transactionId,
        BigDecimal updatedBalance
) {
}

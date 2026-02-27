package br.com.autevia.finkidsapi.service.dto;

import java.math.BigDecimal;
import java.util.List;

public record TransactionListResult(
        BigDecimal currentBalance,
        List<TransactionItemResult> transactions
) {
}

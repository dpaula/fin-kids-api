package br.com.autevia.finkidsapi.web.dto;

import java.math.BigDecimal;
import java.util.List;

public record TransactionListResponse(
        BigDecimal currentBalance,
        List<TransactionItemResponse> transactions
) {
}

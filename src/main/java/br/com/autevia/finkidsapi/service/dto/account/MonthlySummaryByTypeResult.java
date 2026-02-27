package br.com.autevia.finkidsapi.service.dto.account;

import br.com.autevia.finkidsapi.domain.enums.TransactionType;
import java.math.BigDecimal;

public record MonthlySummaryByTypeResult(
        TransactionType type,
        BigDecimal total
) {
}

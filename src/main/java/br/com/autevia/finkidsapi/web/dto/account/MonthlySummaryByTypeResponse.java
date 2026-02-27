package br.com.autevia.finkidsapi.web.dto.account;

import br.com.autevia.finkidsapi.domain.enums.TransactionType;
import java.math.BigDecimal;

public record MonthlySummaryByTypeResponse(
        TransactionType type,
        BigDecimal total
) {
}

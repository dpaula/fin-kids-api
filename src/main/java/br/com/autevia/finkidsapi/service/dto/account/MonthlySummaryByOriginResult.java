package br.com.autevia.finkidsapi.service.dto.account;

import br.com.autevia.finkidsapi.domain.enums.TransactionOrigin;
import java.math.BigDecimal;

public record MonthlySummaryByOriginResult(
        TransactionOrigin origin,
        BigDecimal total
) {
}

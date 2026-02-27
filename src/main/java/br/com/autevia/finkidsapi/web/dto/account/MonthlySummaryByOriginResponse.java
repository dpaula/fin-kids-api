package br.com.autevia.finkidsapi.web.dto.account;

import br.com.autevia.finkidsapi.domain.enums.TransactionOrigin;
import java.math.BigDecimal;

public record MonthlySummaryByOriginResponse(
        TransactionOrigin origin,
        BigDecimal total
) {
}

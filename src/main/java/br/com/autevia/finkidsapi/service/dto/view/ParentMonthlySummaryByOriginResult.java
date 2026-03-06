package br.com.autevia.finkidsapi.service.dto.view;

import br.com.autevia.finkidsapi.domain.enums.TransactionOrigin;
import java.math.BigDecimal;

public record ParentMonthlySummaryByOriginResult(
        TransactionOrigin origin,
        BigDecimal total
) {
}

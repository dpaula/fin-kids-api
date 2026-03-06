package br.com.autevia.finkidsapi.service.dto.view;

import br.com.autevia.finkidsapi.domain.enums.TransactionType;
import java.math.BigDecimal;

public record ParentMonthlySummaryByTypeResult(
        TransactionType type,
        BigDecimal total
) {
}

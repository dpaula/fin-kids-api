package br.com.autevia.finkidsapi.service.dto.view;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ParentMonthlySummaryResult(
        int year,
        int month,
        Instant periodStart,
        Instant periodEnd,
        BigDecimal totalDeposits,
        BigDecimal totalWithdrawals,
        BigDecimal netChange,
        List<ParentMonthlySummaryByTypeResult> totalsByType,
        List<ParentMonthlySummaryByOriginResult> totalsByOrigin
) {
}

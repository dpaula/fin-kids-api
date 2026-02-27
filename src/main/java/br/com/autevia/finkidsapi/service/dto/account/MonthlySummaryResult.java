package br.com.autevia.finkidsapi.service.dto.account;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record MonthlySummaryResult(
        Long accountId,
        int year,
        int month,
        Instant periodStart,
        Instant periodEnd,
        BigDecimal currentBalance,
        BigDecimal totalDeposits,
        BigDecimal totalWithdrawals,
        BigDecimal netChange,
        List<MonthlySummaryByTypeResult> totalsByType,
        List<MonthlySummaryByOriginResult> totalsByOrigin
) {
}

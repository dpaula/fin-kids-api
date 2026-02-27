package br.com.autevia.finkidsapi.web.dto.account;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record MonthlySummaryResponse(
        Long accountId,
        int year,
        int month,
        Instant periodStart,
        Instant periodEnd,
        BigDecimal currentBalance,
        BigDecimal totalDeposits,
        BigDecimal totalWithdrawals,
        BigDecimal netChange,
        List<MonthlySummaryByTypeResponse> totalsByType,
        List<MonthlySummaryByOriginResponse> totalsByOrigin
) {
}

package br.com.autevia.finkidsapi.web.dto.account;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(description = "Resposta com consolidacao mensal da conta.")
public record MonthlySummaryResponse(
        @Schema(description = "Id da conta da crianca", example = "1")
        Long accountId,
        @Schema(description = "Ano de referencia", example = "2026")
        int year,
        @Schema(description = "Mes de referencia", example = "2")
        int month,
        @Schema(description = "Inicio do periodo mensal em UTC", example = "2026-02-01T00:00:00Z")
        Instant periodStart,
        @Schema(description = "Fim do periodo mensal em UTC", example = "2026-03-01T00:00:00Z")
        Instant periodEnd,
        @Schema(description = "Saldo atual da conta", example = "180.00")
        BigDecimal currentBalance,
        @Schema(description = "Total de entradas no mes", example = "250.00")
        BigDecimal totalDeposits,
        @Schema(description = "Total de saidas no mes", example = "70.00")
        BigDecimal totalWithdrawals,
        @Schema(description = "Variacao liquida no mes", example = "180.00")
        BigDecimal netChange,
        @Schema(description = "Totais por tipo de transacao")
        List<MonthlySummaryByTypeResponse> totalsByType,
        @Schema(description = "Totais por origem de transacao")
        List<MonthlySummaryByOriginResponse> totalsByOrigin
) {
}

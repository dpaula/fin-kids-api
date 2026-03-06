package br.com.autevia.finkidsapi.web.dto.view;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(description = "Resumo mensal da conta para a visao dos pais.")
public record ParentMonthlySummaryResponse(
        @Schema(description = "Ano de referencia", example = "2026")
        int year,
        @Schema(description = "Mes de referencia", example = "2")
        int month,
        @Schema(description = "Inicio do periodo em UTC", example = "2026-02-01T00:00:00Z")
        Instant periodStart,
        @Schema(description = "Fim do periodo em UTC", example = "2026-03-01T00:00:00Z")
        Instant periodEnd,
        @Schema(description = "Total de entradas no mes", example = "250.00")
        BigDecimal totalDeposits,
        @Schema(description = "Total de saidas no mes", example = "70.00")
        BigDecimal totalWithdrawals,
        @Schema(description = "Variacao liquida no mes", example = "180.00")
        BigDecimal netChange,
        @Schema(description = "Totais mensais por tipo")
        List<ParentMonthlySummaryByTypeResponse> totalsByType,
        @Schema(description = "Totais mensais por origem")
        List<ParentMonthlySummaryByOriginResponse> totalsByOrigin
) {
}

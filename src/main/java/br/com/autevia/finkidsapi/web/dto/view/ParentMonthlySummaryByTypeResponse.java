package br.com.autevia.finkidsapi.web.dto.view;

import br.com.autevia.finkidsapi.domain.enums.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Total mensal agrupado por tipo de transacao.")
public record ParentMonthlySummaryByTypeResponse(
        @Schema(description = "Tipo da transacao", example = "DEPOSIT")
        TransactionType type,
        @Schema(description = "Total no periodo", example = "250.00")
        BigDecimal total
) {
}

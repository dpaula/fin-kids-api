package br.com.autevia.finkidsapi.web.dto.view;

import br.com.autevia.finkidsapi.domain.enums.TransactionOrigin;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Total mensal agrupado por origem da transacao.")
public record ParentMonthlySummaryByOriginResponse(
        @Schema(description = "Origem da transacao", example = "MANUAL")
        TransactionOrigin origin,
        @Schema(description = "Total no periodo", example = "200.00")
        BigDecimal total
) {
}

package br.com.autevia.finkidsapi.web.dto.view;

import br.com.autevia.finkidsapi.domain.enums.TransactionOrigin;
import br.com.autevia.finkidsapi.domain.enums.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Item de historico detalhado para visao dos pais.")
public record ParentTransactionItemResponse(
        @Schema(description = "Id da transacao", example = "77")
        Long transactionId,
        @Schema(description = "Tipo da transacao", example = "WITHDRAW")
        TransactionType type,
        @Schema(description = "Origem da transacao", example = "WHATSAPP")
        TransactionOrigin origin,
        @Schema(description = "Valor da transacao", example = "40.00")
        BigDecimal amount,
        @Schema(description = "Descricao curta", example = "Compra de caderno")
        String description,
        @Schema(description = "Referencia de evidencia da transacao", example = "wa-media-001")
        String evidenceReference,
        @Schema(description = "Data/hora da transacao em UTC", example = "2026-02-20T10:00:00Z")
        Instant occurredAt
) {
}

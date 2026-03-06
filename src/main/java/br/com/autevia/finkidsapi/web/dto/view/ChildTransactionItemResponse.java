package br.com.autevia.finkidsapi.web.dto.view;

import br.com.autevia.finkidsapi.domain.enums.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Item de historico simplificado para visao da crianca.")
public record ChildTransactionItemResponse(
        @Schema(description = "Id da transacao", example = "77")
        Long transactionId,
        @Schema(description = "Tipo da transacao", example = "DEPOSIT")
        TransactionType type,
        @Schema(description = "Valor da transacao", example = "40.00")
        BigDecimal amount,
        @Schema(description = "Descricao curta", example = "Mesada")
        String description,
        @Schema(description = "Data/hora da transacao em UTC", example = "2026-02-20T10:00:00Z")
        Instant occurredAt
) {
}

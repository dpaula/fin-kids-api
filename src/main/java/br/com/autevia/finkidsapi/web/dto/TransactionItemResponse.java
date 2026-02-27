package br.com.autevia.finkidsapi.web.dto;

import br.com.autevia.finkidsapi.domain.enums.TransactionOrigin;
import br.com.autevia.finkidsapi.domain.enums.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Item de transacao no extrato.")
public record TransactionItemResponse(
        @Schema(description = "Id da transacao", example = "77")
        Long transactionId,
        @Schema(description = "Id da conta da crianca", example = "1")
        Long accountId,
        @Schema(description = "Tipo da transacao", example = "DEPOSIT")
        TransactionType type,
        @Schema(description = "Origem da transacao", example = "MANUAL")
        TransactionOrigin origin,
        @Schema(description = "Valor da transacao", example = "40.00")
        BigDecimal amount,
        @Schema(description = "Descricao da transacao", example = "Mesada")
        String description,
        @Schema(description = "Referencia da evidencia associada", example = "media-id-123")
        String evidenceReference,
        @Schema(description = "Data/hora de ocorrencia em UTC", example = "2026-02-20T10:00:00Z")
        Instant occurredAt
) {
}

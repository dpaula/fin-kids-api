package br.com.autevia.finkidsapi.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Resposta de criacao de transacao.")
public record CreateTransactionResponse(
        @Schema(description = "Id da transacao criada", example = "100")
        Long transactionId,
        @Schema(description = "Saldo atualizado apos criacao", example = "150.00")
        BigDecimal updatedBalance
) {
}

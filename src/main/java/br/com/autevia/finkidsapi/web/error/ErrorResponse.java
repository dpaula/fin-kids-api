package br.com.autevia.finkidsapi.web.error;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(name = "ErrorResponse", description = "Modelo padrao de erro da API.")
public record ErrorResponse(
        @Schema(description = "Data/hora da ocorrencia do erro em UTC", example = "2026-02-27T13:00:00Z")
        Instant timestamp,
        @Schema(description = "Codigo HTTP do erro", example = "400")
        int status,
        @Schema(description = "Descricao curta do status HTTP", example = "Bad Request")
        String error,
        @Schema(description = "Mensagem detalhada do erro", example = "accountId deve ser maior que zero.")
        String message,
        @Schema(description = "Path chamado na API", example = "/api/v1/transactions")
        String path
) {
}

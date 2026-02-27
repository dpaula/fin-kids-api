package br.com.autevia.finkidsapi.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Resposta de listagem de transacoes por periodo.")
public record TransactionListResponse(
        @Schema(description = "Saldo atual da conta", example = "90.00")
        BigDecimal currentBalance,
        @Schema(description = "Lista de transacoes do periodo")
        List<TransactionItemResponse> transactions
) {
}

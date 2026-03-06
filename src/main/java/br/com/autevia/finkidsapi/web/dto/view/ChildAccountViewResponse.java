package br.com.autevia.finkidsapi.web.dto.view;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Visao principal da conta para a tela da crianca.")
public record ChildAccountViewResponse(
        @Schema(description = "Id da conta da crianca", example = "1")
        Long accountId,
        @Schema(description = "Nome da crianca", example = "Lucas")
        String childName,
        @Schema(description = "Codigo da moeda da conta", example = "BRL")
        String currencyCode,
        @Schema(description = "Saldo atual calculado pelo historico", example = "180.00")
        BigDecimal currentBalance,
        @Schema(description = "Metas com progresso por saldo")
        List<GoalProgressResponse> goals,
        @Schema(description = "Ultimas transacoes simplificadas")
        List<ChildTransactionItemResponse> recentTransactions
) {
}

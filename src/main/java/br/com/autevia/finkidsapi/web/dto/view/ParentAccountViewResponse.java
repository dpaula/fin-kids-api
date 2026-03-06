package br.com.autevia.finkidsapi.web.dto.view;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Visao consolidada da conta para a tela dos pais.")
public record ParentAccountViewResponse(
        @Schema(description = "Id da conta da crianca", example = "1")
        Long accountId,
        @Schema(description = "Nome da crianca", example = "Lucas")
        String childName,
        @Schema(description = "Codigo da moeda da conta", example = "BRL")
        String currencyCode,
        @Schema(description = "Saldo atual calculado pelo historico", example = "180.00")
        BigDecimal currentBalance,
        @Schema(description = "Resumo mensal no periodo solicitado")
        ParentMonthlySummaryResponse monthlySummary,
        @Schema(description = "Regra de bonus ativa na conta (pode ser nula quando nao configurada)")
        ParentBonusRuleResponse bonusRule,
        @Schema(description = "Metas com progresso por saldo")
        List<GoalProgressResponse> goals,
        @Schema(description = "Ultimas transacoes detalhadas")
        List<ParentTransactionItemResponse> recentTransactions
) {
}

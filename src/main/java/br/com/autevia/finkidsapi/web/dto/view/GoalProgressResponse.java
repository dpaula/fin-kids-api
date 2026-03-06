package br.com.autevia.finkidsapi.web.dto.view;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Meta com progresso calculado pelo saldo atual.")
public record GoalProgressResponse(
        @Schema(description = "Id da meta", example = "11")
        Long goalId,
        @Schema(description = "Nome da meta", example = "Bicicleta")
        String name,
        @Schema(description = "Valor alvo da meta", example = "500.00")
        BigDecimal targetAmount,
        @Schema(description = "Valor ja avancado na meta pelo saldo atual", example = "320.00")
        BigDecimal progressAmount,
        @Schema(description = "Percentual de progresso da meta", example = "64.00")
        BigDecimal progressPercent,
        @Schema(description = "Valor restante para atingir a meta", example = "180.00")
        BigDecimal remainingAmount,
        @Schema(description = "Indica se a meta ja foi atingida", example = "false")
        boolean achieved
) {
}

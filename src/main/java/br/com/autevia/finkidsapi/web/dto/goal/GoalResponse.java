package br.com.autevia.finkidsapi.web.dto.goal;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Resposta de meta.")
public record GoalResponse(
        @Schema(description = "Id da meta", example = "11")
        Long goalId,
        @Schema(description = "Id da conta da crianca", example = "1")
        Long accountId,
        @Schema(description = "Nome da meta", example = "Bicicleta")
        String name,
        @Schema(description = "Valor alvo da meta", example = "500.00")
        BigDecimal targetAmount,
        @Schema(description = "Indica se a meta esta ativa", example = "true")
        boolean active,
        @Schema(description = "Data de criacao da meta em UTC", example = "2026-02-27T12:00:00Z")
        Instant createdAt,
        @Schema(description = "Data da ultima atualizacao em UTC", example = "2026-02-27T12:00:00Z")
        Instant updatedAt
) {
}

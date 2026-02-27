package br.com.autevia.finkidsapi.web.dto.bonus;

import br.com.autevia.finkidsapi.domain.enums.BonusBaseType;
import br.com.autevia.finkidsapi.domain.enums.BonusConditionType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Resposta da regra de bonus da conta.")
public record BonusRuleResponse(
        @Schema(description = "Id da regra de bonus", example = "15")
        Long bonusRuleId,
        @Schema(description = "Id da conta da crianca", example = "1")
        Long accountId,
        @Schema(description = "Percentual de bonus configurado", example = "10.00")
        BigDecimal percentage,
        @Schema(description = "Condicao da regra", example = "NO_WITHDRAWALS_IN_MONTH")
        BonusConditionType conditionType,
        @Schema(description = "Base de calculo do bonus", example = "MONTHLY_DEPOSITS")
        BonusBaseType baseType,
        @Schema(description = "Indica se a regra esta ativa", example = "true")
        boolean active,
        @Schema(description = "Data de criacao em UTC", example = "2026-02-27T14:00:00Z")
        Instant createdAt,
        @Schema(description = "Data da ultima atualizacao em UTC", example = "2026-02-27T14:00:00Z")
        Instant updatedAt
) {
}

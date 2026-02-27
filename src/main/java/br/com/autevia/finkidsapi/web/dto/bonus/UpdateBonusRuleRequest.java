package br.com.autevia.finkidsapi.web.dto.bonus;

import br.com.autevia.finkidsapi.domain.enums.BonusBaseType;
import br.com.autevia.finkidsapi.domain.enums.BonusConditionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "Payload para criacao/atualizacao de regra de bonus.")
public record UpdateBonusRuleRequest(
        @Schema(description = "Percentual de bonus aplicado", example = "10.00")
        @NotNull(message = "percentage e obrigatorio.")
        @DecimalMin(value = "0.01", message = "percentage deve estar entre 0.01 e 100.00.")
        @DecimalMax(value = "100.00", message = "percentage deve estar entre 0.01 e 100.00.")
        BigDecimal percentage,
        @Schema(description = "Condicao de elegibilidade do bonus", example = "NO_WITHDRAWALS_IN_MONTH")
        @NotNull(message = "conditionType e obrigatorio.")
        BonusConditionType conditionType,
        @Schema(description = "Base de calculo do bonus", example = "MONTHLY_DEPOSITS")
        @NotNull(message = "baseType e obrigatorio.")
        BonusBaseType baseType,
        @Schema(description = "Indica se a regra esta ativa", example = "true")
        @NotNull(message = "active e obrigatorio.")
        Boolean active
) {
}

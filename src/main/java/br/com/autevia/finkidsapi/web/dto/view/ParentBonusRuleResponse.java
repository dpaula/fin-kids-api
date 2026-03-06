package br.com.autevia.finkidsapi.web.dto.view;

import br.com.autevia.finkidsapi.domain.enums.BonusBaseType;
import br.com.autevia.finkidsapi.domain.enums.BonusConditionType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Regra de bonus configurada para a conta.")
public record ParentBonusRuleResponse(
        @Schema(description = "Percentual de bonus", example = "5.00")
        BigDecimal percentage,
        @Schema(description = "Condicao para aplicacao do bonus", example = "NO_WITHDRAWALS_IN_MONTH")
        BonusConditionType conditionType,
        @Schema(description = "Base de calculo do bonus", example = "MONTHLY_DEPOSITS")
        BonusBaseType baseType,
        @Schema(description = "Indica se a regra esta ativa", example = "true")
        boolean active
) {
}

package br.com.autevia.finkidsapi.web.dto.bonus;

import br.com.autevia.finkidsapi.domain.enums.BonusBaseType;
import br.com.autevia.finkidsapi.domain.enums.BonusConditionType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record UpdateBonusRuleRequest(
        @NotNull(message = "percentage e obrigatorio.")
        @DecimalMin(value = "0.01", message = "percentage deve estar entre 0.01 e 100.00.")
        @DecimalMax(value = "100.00", message = "percentage deve estar entre 0.01 e 100.00.")
        BigDecimal percentage,
        @NotNull(message = "conditionType e obrigatorio.")
        BonusConditionType conditionType,
        @NotNull(message = "baseType e obrigatorio.")
        BonusBaseType baseType,
        @NotNull(message = "active e obrigatorio.")
        Boolean active
) {
}

package br.com.autevia.finkidsapi.service.dto.bonus;

import br.com.autevia.finkidsapi.domain.enums.BonusBaseType;
import br.com.autevia.finkidsapi.domain.enums.BonusConditionType;
import java.math.BigDecimal;

public record UpdateBonusRuleCommand(
        BigDecimal percentage,
        BonusConditionType conditionType,
        BonusBaseType baseType,
        Boolean active
) {
}

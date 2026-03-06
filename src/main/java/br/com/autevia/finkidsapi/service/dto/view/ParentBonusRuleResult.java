package br.com.autevia.finkidsapi.service.dto.view;

import br.com.autevia.finkidsapi.domain.enums.BonusBaseType;
import br.com.autevia.finkidsapi.domain.enums.BonusConditionType;
import java.math.BigDecimal;

public record ParentBonusRuleResult(
        BigDecimal percentage,
        BonusConditionType conditionType,
        BonusBaseType baseType,
        boolean active
) {
}

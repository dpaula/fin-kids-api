package br.com.autevia.finkidsapi.web.dto.bonus;

import br.com.autevia.finkidsapi.domain.enums.BonusBaseType;
import br.com.autevia.finkidsapi.domain.enums.BonusConditionType;
import java.math.BigDecimal;
import java.time.Instant;

public record BonusRuleResponse(
        Long bonusRuleId,
        Long accountId,
        BigDecimal percentage,
        BonusConditionType conditionType,
        BonusBaseType baseType,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}

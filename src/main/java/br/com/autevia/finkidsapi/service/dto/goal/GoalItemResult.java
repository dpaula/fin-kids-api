package br.com.autevia.finkidsapi.service.dto.goal;

import java.math.BigDecimal;
import java.time.Instant;

public record GoalItemResult(
        Long goalId,
        Long accountId,
        String name,
        BigDecimal targetAmount,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}

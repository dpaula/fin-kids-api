package br.com.autevia.finkidsapi.web.dto.goal;

import java.math.BigDecimal;
import java.time.Instant;

public record GoalResponse(
        Long goalId,
        Long accountId,
        String name,
        BigDecimal targetAmount,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}

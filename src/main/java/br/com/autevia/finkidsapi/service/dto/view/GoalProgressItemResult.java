package br.com.autevia.finkidsapi.service.dto.view;

import java.math.BigDecimal;

public record GoalProgressItemResult(
        Long goalId,
        String name,
        BigDecimal targetAmount,
        BigDecimal progressAmount,
        BigDecimal progressPercent,
        BigDecimal remainingAmount,
        boolean achieved
) {
}

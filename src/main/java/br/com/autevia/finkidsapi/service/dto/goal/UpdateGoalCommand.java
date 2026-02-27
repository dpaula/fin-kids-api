package br.com.autevia.finkidsapi.service.dto.goal;

import java.math.BigDecimal;

public record UpdateGoalCommand(
        Long accountId,
        String name,
        BigDecimal targetAmount
) {
}

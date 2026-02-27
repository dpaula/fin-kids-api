package br.com.autevia.finkidsapi.service.dto.goal;

import java.math.BigDecimal;

public record CreateGoalCommand(
        Long accountId,
        String name,
        BigDecimal targetAmount
) {
}

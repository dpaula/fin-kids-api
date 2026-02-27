package br.com.autevia.finkidsapi.web.dto.goal;

import java.math.BigDecimal;

public record UpdateGoalRequest(
        Long accountId,
        String name,
        BigDecimal targetAmount
) {
}

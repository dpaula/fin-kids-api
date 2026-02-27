package br.com.autevia.finkidsapi.service.dto.account;

import java.math.BigDecimal;

public record AccountBalanceResult(
        Long accountId,
        BigDecimal currentBalance
) {
}

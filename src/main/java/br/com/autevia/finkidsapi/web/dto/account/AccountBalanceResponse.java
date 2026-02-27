package br.com.autevia.finkidsapi.web.dto.account;

import java.math.BigDecimal;

public record AccountBalanceResponse(
        Long accountId,
        BigDecimal currentBalance
) {
}

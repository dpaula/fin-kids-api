package br.com.autevia.finkidsapi.web.dto.account;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Resposta com saldo atual da conta.")
public record AccountBalanceResponse(
        @Schema(description = "Id da conta da crianca", example = "1")
        Long accountId,
        @Schema(description = "Saldo atual calculado pelo historico", example = "123.45")
        BigDecimal currentBalance
) {
}

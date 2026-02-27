package br.com.autevia.finkidsapi.web.dto;

import br.com.autevia.finkidsapi.domain.enums.TransactionOrigin;
import br.com.autevia.finkidsapi.domain.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public record CreateTransactionRequest(
        @NotNull(message = "accountId e obrigatorio.")
        @Positive(message = "accountId deve ser maior que zero.")
        Long accountId,
        @NotNull(message = "type e obrigatorio.")
        TransactionType type,
        @NotNull(message = "origin e obrigatorio.")
        TransactionOrigin origin,
        @NotNull(message = "amount e obrigatorio.")
        @DecimalMin(value = "0.01", message = "amount deve ser maior que zero.")
        BigDecimal amount,
        @NotBlank(message = "description e obrigatoria.")
        @Size(max = 255, message = "description deve ter no maximo 255 caracteres.")
        String description,
        String evidenceReference,
        Instant occurredAt
) {
}

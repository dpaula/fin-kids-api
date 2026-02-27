package br.com.autevia.finkidsapi.web.dto.goal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateGoalRequest(
        @NotNull(message = "accountId e obrigatorio.")
        @Positive(message = "accountId deve ser maior que zero.")
        Long accountId,
        @NotBlank(message = "name e obrigatorio.")
        @Size(max = 120, message = "name deve ter no maximo 120 caracteres.")
        String name,
        @NotNull(message = "targetAmount e obrigatorio.")
        @DecimalMin(value = "0.01", message = "targetAmount deve ser maior que zero.")
        BigDecimal targetAmount
) {
}

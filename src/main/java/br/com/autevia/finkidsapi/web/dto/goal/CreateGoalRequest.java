package br.com.autevia.finkidsapi.web.dto.goal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(description = "Payload para criacao de meta.")
public record CreateGoalRequest(
        @Schema(description = "Id da conta da crianca", example = "1")
        @NotNull(message = "accountId e obrigatorio.")
        @Positive(message = "accountId deve ser maior que zero.")
        Long accountId,
        @Schema(description = "Nome da meta", example = "Bicicleta")
        @NotBlank(message = "name e obrigatorio.")
        @Size(max = 120, message = "name deve ter no maximo 120 caracteres.")
        String name,
        @Schema(description = "Valor alvo da meta", example = "500.00")
        @NotNull(message = "targetAmount e obrigatorio.")
        @DecimalMin(value = "0.01", message = "targetAmount deve ser maior que zero.")
        BigDecimal targetAmount
) {
}

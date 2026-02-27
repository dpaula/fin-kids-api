package br.com.autevia.finkidsapi.web.dto.automation;

import br.com.autevia.finkidsapi.domain.enums.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Payload de transacao recebida via automacao n8n/WhatsApp.")
public record CreateAutomationTransactionRequest(
        @Schema(description = "Id da conta da crianca", example = "1")
        @NotNull(message = "accountId e obrigatorio.")
        @Positive(message = "accountId deve ser maior que zero.")
        Long accountId,
        @Schema(description = "Tipo da transacao", example = "DEPOSIT")
        @NotNull(message = "type e obrigatorio.")
        TransactionType type,
        @Schema(description = "Valor da transacao", example = "100.00")
        @NotNull(message = "amount e obrigatorio.")
        @DecimalMin(value = "0.01", message = "amount deve ser maior que zero.")
        BigDecimal amount,
        @Schema(description = "Descricao curta da transacao", example = "Deposito identificado via comprovante")
        @NotBlank(message = "description e obrigatoria.")
        @Size(max = 255, message = "description deve ter no maximo 255 caracteres.")
        String description,
        @Schema(description = "Referencia da evidencia recebida da automacao", example = "wa-media-001")
        String evidenceReference,
        @Schema(description = "Data/hora da ocorrencia em UTC", example = "2026-02-27T12:00:00Z")
        Instant occurredAt
) {
}

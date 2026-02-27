package br.com.autevia.finkidsapi.web;

import br.com.autevia.finkidsapi.domain.enums.TransactionOrigin;
import br.com.autevia.finkidsapi.service.TransactionService;
import br.com.autevia.finkidsapi.service.dto.CreateTransactionCommand;
import br.com.autevia.finkidsapi.service.dto.CreateTransactionResult;
import br.com.autevia.finkidsapi.web.dto.CreateTransactionResponse;
import br.com.autevia.finkidsapi.web.dto.automation.CreateAutomationTransactionRequest;
import br.com.autevia.finkidsapi.web.error.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/automation")
@Validated
@Tag(name = "Automation", description = "Endpoints protegidos para integracao com n8n/WhatsApp.")
public class AutomationTransactionController {

    private final TransactionService transactionService;

    public AutomationTransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Registrar transacao via automacao",
            description = "Registra transacao autenticada por token dedicado. A origem e sempre WHATSAPP.",
            security = @SecurityRequirement(name = "AutomationBearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Transacao criada com sucesso",
                    content = @Content(schema = @Schema(implementation = CreateTransactionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Payload invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token ausente ou invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Conta nao encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Regra de negocio violada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public CreateTransactionResponse createViaAutomation(@Valid @RequestBody CreateAutomationTransactionRequest request) {
        CreateTransactionResult result = transactionService.createTransaction(
                new CreateTransactionCommand(
                        request.accountId(),
                        request.type(),
                        TransactionOrigin.WHATSAPP,
                        request.amount(),
                        request.description(),
                        request.evidenceReference(),
                        request.occurredAt()
                )
        );

        return new CreateTransactionResponse(result.transactionId(), result.updatedBalance());
    }
}

package br.com.autevia.finkidsapi.web;

import br.com.autevia.finkidsapi.service.TransactionService;
import br.com.autevia.finkidsapi.service.dto.CreateTransactionCommand;
import br.com.autevia.finkidsapi.service.dto.CreateTransactionResult;
import br.com.autevia.finkidsapi.service.dto.TransactionItemResult;
import br.com.autevia.finkidsapi.service.dto.TransactionListResult;
import br.com.autevia.finkidsapi.web.dto.CreateTransactionRequest;
import br.com.autevia.finkidsapi.web.dto.CreateTransactionResponse;
import br.com.autevia.finkidsapi.web.dto.TransactionItemResponse;
import br.com.autevia.finkidsapi.web.dto.TransactionListResponse;
import br.com.autevia.finkidsapi.web.error.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
@Validated
@Tag(name = "Transactions", description = "Operacoes de criacao e consulta de transacoes financeiras da conta.")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar transacao", description = "Cria uma transacao de deposito ou saque e retorna o saldo atualizado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Transacao criada com sucesso",
                    content = @Content(schema = @Schema(implementation = CreateTransactionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Payload invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Transacao duplicada para a mesma evidencia",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Conta nao encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Regra de negocio violada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public CreateTransactionResponse create(@Valid @RequestBody CreateTransactionRequest request) {
        CreateTransactionResult result = transactionService.createTransaction(
                new CreateTransactionCommand(
                        request.accountId(),
                        request.type(),
                        request.origin(),
                        request.amount(),
                        request.description(),
                        request.evidenceReference(),
                        request.occurredAt()
                )
        );

        return new CreateTransactionResponse(result.transactionId(), result.updatedBalance());
    }

    @GetMapping
    @Operation(summary = "Listar transacoes por periodo", description = "Retorna transacoes do periodo informado e o saldo atual.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transacoes listadas com sucesso",
                    content = @Content(schema = @Schema(implementation = TransactionListResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parametros invalidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Conta nao encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public TransactionListResponse list(
            @Parameter(description = "Id da conta da crianca", example = "1")
            @RequestParam @Positive(message = "accountId deve ser maior que zero.") Long accountId,
            @Parameter(description = "Data/hora inicial do intervalo em UTC", example = "2026-02-01T00:00:00Z")
            @RequestParam Instant start,
            @Parameter(description = "Data/hora final do intervalo em UTC", example = "2026-02-28T23:59:59Z")
            @RequestParam Instant end
    ) {
        TransactionListResult result = transactionService.listTransactions(accountId, start, end);
        List<TransactionItemResponse> items = result.transactions().stream()
                .map(this::toResponse)
                .toList();

        return new TransactionListResponse(result.currentBalance(), items);
    }

    private TransactionItemResponse toResponse(TransactionItemResult item) {
        return new TransactionItemResponse(
                item.transactionId(),
                item.accountId(),
                item.type(),
                item.origin(),
                item.amount(),
                item.description(),
                item.evidenceReference(),
                item.occurredAt()
        );
    }
}

package br.com.autevia.finkidsapi.web;

import br.com.autevia.finkidsapi.service.AccountSummaryService;
import br.com.autevia.finkidsapi.service.dto.account.AccountBalanceResult;
import br.com.autevia.finkidsapi.service.dto.account.MonthlySummaryByOriginResult;
import br.com.autevia.finkidsapi.service.dto.account.MonthlySummaryByTypeResult;
import br.com.autevia.finkidsapi.service.dto.account.MonthlySummaryResult;
import br.com.autevia.finkidsapi.web.dto.account.AccountBalanceResponse;
import br.com.autevia.finkidsapi.web.dto.account.MonthlySummaryByOriginResponse;
import br.com.autevia.finkidsapi.web.dto.account.MonthlySummaryByTypeResponse;
import br.com.autevia.finkidsapi.web.dto.account.MonthlySummaryResponse;
import br.com.autevia.finkidsapi.web.error.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
@Validated
@Tag(name = "Accounts", description = "Consultas de saldo atual e consolidacao mensal da conta.")
public class AccountSummaryController {

    private final AccountSummaryService accountSummaryService;

    public AccountSummaryController(AccountSummaryService accountSummaryService) {
        this.accountSummaryService = accountSummaryService;
    }

    @GetMapping("/{accountId}/balance")
    @Operation(summary = "Consultar saldo atual", description = "Retorna o saldo atual calculado pelo historico de transacoes.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Saldo retornado com sucesso",
                    content = @Content(schema = @Schema(implementation = AccountBalanceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parametro invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Conta nao encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public AccountBalanceResponse getBalance(
            @Parameter(description = "Id da conta da crianca", example = "1")
            @PathVariable @Positive(message = "accountId deve ser maior que zero.") Long accountId
    ) {
        AccountBalanceResult result = accountSummaryService.getBalance(accountId);
        return new AccountBalanceResponse(result.accountId(), result.currentBalance());
    }

    @GetMapping("/{accountId}/monthly-summary")
    @Operation(summary = "Consultar resumo mensal", description = "Retorna totais por tipo e origem para o mes informado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Resumo mensal retornado com sucesso",
                    content = @Content(schema = @Schema(implementation = MonthlySummaryResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parametros invalidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Conta nao encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public MonthlySummaryResponse getMonthlySummary(
            @Parameter(description = "Id da conta da crianca", example = "1")
            @PathVariable @Positive(message = "accountId deve ser maior que zero.") Long accountId,
            @Parameter(description = "Ano de referencia do resumo", example = "2026")
            @RequestParam @Min(value = 2000, message = "year deve estar entre 2000 e 2100.")
            @Max(value = 2100, message = "year deve estar entre 2000 e 2100.") int year,
            @Parameter(description = "Mes de referencia do resumo", example = "2")
            @RequestParam @Min(value = 1, message = "month deve estar entre 1 e 12.")
            @Max(value = 12, message = "month deve estar entre 1 e 12.") int month
    ) {
        MonthlySummaryResult result = accountSummaryService.getMonthlySummary(accountId, year, month);

        List<MonthlySummaryByTypeResponse> typeItems = result.totalsByType().stream()
                .map(this::toTypeResponse)
                .toList();

        List<MonthlySummaryByOriginResponse> originItems = result.totalsByOrigin().stream()
                .map(this::toOriginResponse)
                .toList();

        return new MonthlySummaryResponse(
                result.accountId(),
                result.year(),
                result.month(),
                result.periodStart(),
                result.periodEnd(),
                result.currentBalance(),
                result.totalDeposits(),
                result.totalWithdrawals(),
                result.netChange(),
                typeItems,
                originItems
        );
    }

    private MonthlySummaryByTypeResponse toTypeResponse(MonthlySummaryByTypeResult item) {
        return new MonthlySummaryByTypeResponse(item.type(), item.total());
    }

    private MonthlySummaryByOriginResponse toOriginResponse(MonthlySummaryByOriginResult item) {
        return new MonthlySummaryByOriginResponse(item.origin(), item.total());
    }
}

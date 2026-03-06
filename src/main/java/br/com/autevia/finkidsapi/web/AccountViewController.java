package br.com.autevia.finkidsapi.web;

import br.com.autevia.finkidsapi.service.AccountViewService;
import br.com.autevia.finkidsapi.service.dto.view.ChildAccountViewResult;
import br.com.autevia.finkidsapi.service.dto.view.ChildTransactionItemResult;
import br.com.autevia.finkidsapi.service.dto.view.GoalProgressItemResult;
import br.com.autevia.finkidsapi.service.dto.view.ParentAccountViewResult;
import br.com.autevia.finkidsapi.service.dto.view.ParentBonusRuleResult;
import br.com.autevia.finkidsapi.service.dto.view.ParentMonthlySummaryByOriginResult;
import br.com.autevia.finkidsapi.service.dto.view.ParentMonthlySummaryByTypeResult;
import br.com.autevia.finkidsapi.service.dto.view.ParentMonthlySummaryResult;
import br.com.autevia.finkidsapi.service.dto.view.ParentTransactionItemResult;
import br.com.autevia.finkidsapi.web.dto.view.ChildAccountViewResponse;
import br.com.autevia.finkidsapi.web.dto.view.ChildTransactionItemResponse;
import br.com.autevia.finkidsapi.web.dto.view.GoalProgressResponse;
import br.com.autevia.finkidsapi.web.dto.view.ParentAccountViewResponse;
import br.com.autevia.finkidsapi.web.dto.view.ParentBonusRuleResponse;
import br.com.autevia.finkidsapi.web.dto.view.ParentMonthlySummaryByOriginResponse;
import br.com.autevia.finkidsapi.web.dto.view.ParentMonthlySummaryByTypeResponse;
import br.com.autevia.finkidsapi.web.dto.view.ParentMonthlySummaryResponse;
import br.com.autevia.finkidsapi.web.dto.view.ParentTransactionItemResponse;
import br.com.autevia.finkidsapi.web.error.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
@Validated
@SecurityRequirement(name = "UserBearerAuth")
@Tag(name = "Account Views", description = "Consultas consolidadas para telas da crianca e dos pais.")
public class AccountViewController {

    private final AccountViewService accountViewService;

    public AccountViewController(AccountViewService accountViewService) {
        this.accountViewService = accountViewService;
    }

    @GetMapping("/{accountId}/child-view")
    @Operation(
            summary = "Consultar visao da crianca",
            description = "Retorna saldo, metas com progresso e historico simplificado para a tela da crianca."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Visao retornada com sucesso",
                    content = @Content(schema = @Schema(implementation = ChildAccountViewResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parametros invalidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Conta nao encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("@accountAuthorization.canRead(#accountId)")
    public ChildAccountViewResponse getChildView(
            @Parameter(description = "Id da conta da crianca", example = "1")
            @PathVariable @Positive(message = "accountId deve ser maior que zero.") Long accountId,
            @Parameter(description = "Quantidade de transacoes recentes no historico simplificado", example = "8")
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "recentTransactionsLimit deve estar entre 1 e 50.")
            @Max(value = 50, message = "recentTransactionsLimit deve estar entre 1 e 50.")
            int recentTransactionsLimit
    ) {
        ChildAccountViewResult result = accountViewService.getChildView(accountId, recentTransactionsLimit);
        return new ChildAccountViewResponse(
                result.accountId(),
                result.childName(),
                result.currencyCode(),
                result.currentBalance(),
                result.goals().stream().map(this::toGoalProgressResponse).toList(),
                result.recentTransactions().stream().map(this::toChildTransactionResponse).toList()
        );
    }

    @GetMapping("/{accountId}/parent-view")
    @Operation(
            summary = "Consultar visao dos pais",
            description = "Retorna saldo, resumo mensal, regra de bonus, metas e historico detalhado da conta."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Visao retornada com sucesso",
                    content = @Content(schema = @Schema(implementation = ParentAccountViewResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parametros invalidos",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Conta nao encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("@accountAuthorization.canRead(#accountId)")
    public ParentAccountViewResponse getParentView(
            @Parameter(description = "Id da conta da crianca", example = "1")
            @PathVariable @Positive(message = "accountId deve ser maior que zero.") Long accountId,
            @Parameter(description = "Ano de referencia para o resumo mensal", example = "2026")
            @RequestParam @Min(value = 2000, message = "year deve estar entre 2000 e 2100.")
            @Max(value = 2100, message = "year deve estar entre 2000 e 2100.") int year,
            @Parameter(description = "Mes de referencia para o resumo mensal", example = "2")
            @RequestParam @Min(value = 1, message = "month deve estar entre 1 e 12.")
            @Max(value = 12, message = "month deve estar entre 1 e 12.") int month,
            @Parameter(description = "Quantidade de transacoes recentes no historico detalhado", example = "20")
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "recentTransactionsLimit deve estar entre 1 e 50.")
            @Max(value = 50, message = "recentTransactionsLimit deve estar entre 1 e 50.")
            int recentTransactionsLimit
    ) {
        ParentAccountViewResult result = accountViewService.getParentView(
                accountId,
                year,
                month,
                recentTransactionsLimit
        );

        return new ParentAccountViewResponse(
                result.accountId(),
                result.childName(),
                result.currencyCode(),
                result.currentBalance(),
                toParentMonthlySummaryResponse(result.monthlySummary()),
                toParentBonusRuleResponse(result.bonusRule()),
                result.goals().stream().map(this::toGoalProgressResponse).toList(),
                result.recentTransactions().stream().map(this::toParentTransactionResponse).toList()
        );
    }

    private GoalProgressResponse toGoalProgressResponse(GoalProgressItemResult item) {
        return new GoalProgressResponse(
                item.goalId(),
                item.name(),
                item.targetAmount(),
                item.progressAmount(),
                item.progressPercent(),
                item.remainingAmount(),
                item.achieved()
        );
    }

    private ChildTransactionItemResponse toChildTransactionResponse(ChildTransactionItemResult item) {
        return new ChildTransactionItemResponse(
                item.transactionId(),
                item.type(),
                item.amount(),
                item.description(),
                item.occurredAt()
        );
    }

    private ParentMonthlySummaryResponse toParentMonthlySummaryResponse(ParentMonthlySummaryResult result) {
        if (result == null) {
            return null;
        }
        List<ParentMonthlySummaryByTypeResponse> totalsByType = result.totalsByType().stream()
                .map(this::toParentMonthlySummaryByTypeResponse)
                .toList();
        List<ParentMonthlySummaryByOriginResponse> totalsByOrigin = result.totalsByOrigin().stream()
                .map(this::toParentMonthlySummaryByOriginResponse)
                .toList();
        return new ParentMonthlySummaryResponse(
                result.year(),
                result.month(),
                result.periodStart(),
                result.periodEnd(),
                result.totalDeposits(),
                result.totalWithdrawals(),
                result.netChange(),
                totalsByType,
                totalsByOrigin
        );
    }

    private ParentMonthlySummaryByTypeResponse toParentMonthlySummaryByTypeResponse(
            ParentMonthlySummaryByTypeResult item
    ) {
        return new ParentMonthlySummaryByTypeResponse(item.type(), item.total());
    }

    private ParentMonthlySummaryByOriginResponse toParentMonthlySummaryByOriginResponse(
            ParentMonthlySummaryByOriginResult item
    ) {
        return new ParentMonthlySummaryByOriginResponse(item.origin(), item.total());
    }

    private ParentBonusRuleResponse toParentBonusRuleResponse(ParentBonusRuleResult result) {
        if (result == null) {
            return null;
        }
        return new ParentBonusRuleResponse(
                result.percentage(),
                result.conditionType(),
                result.baseType(),
                result.active()
        );
    }

    private ParentTransactionItemResponse toParentTransactionResponse(ParentTransactionItemResult item) {
        return new ParentTransactionItemResponse(
                item.transactionId(),
                item.type(),
                item.origin(),
                item.amount(),
                item.description(),
                item.evidenceReference(),
                item.occurredAt()
        );
    }
}

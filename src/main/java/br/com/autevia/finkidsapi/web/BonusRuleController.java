package br.com.autevia.finkidsapi.web;

import br.com.autevia.finkidsapi.service.BonusRuleService;
import br.com.autevia.finkidsapi.service.dto.bonus.BonusRuleResult;
import br.com.autevia.finkidsapi.service.dto.bonus.UpdateBonusRuleCommand;
import br.com.autevia.finkidsapi.web.dto.bonus.BonusRuleResponse;
import br.com.autevia.finkidsapi.web.dto.bonus.UpdateBonusRuleRequest;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
@Validated
@Tag(name = "Bonus Rules", description = "Consulta e configuracao de regra de bonus por conta.")
public class BonusRuleController {

    private final BonusRuleService bonusRuleService;

    public BonusRuleController(BonusRuleService bonusRuleService) {
        this.bonusRuleService = bonusRuleService;
    }

    @GetMapping("/{accountId}/bonus-rule")
    @Operation(summary = "Consultar regra de bonus", description = "Retorna a regra de bonus configurada para a conta.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Regra retornada com sucesso",
                    content = @Content(schema = @Schema(implementation = BonusRuleResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parametro invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Conta ou regra nao encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public BonusRuleResponse getRule(
            @Parameter(description = "Id da conta da crianca", example = "1")
            @PathVariable @Positive(message = "accountId deve ser maior que zero.") Long accountId
    ) {
        BonusRuleResult result = bonusRuleService.getRule(accountId);
        return toResponse(result);
    }

    @PutMapping("/{accountId}/bonus-rule")
    @Operation(summary = "Criar ou atualizar regra de bonus", description = "Atualiza a regra da conta ou cria quando ainda nao existe (upsert).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Regra salva com sucesso",
                    content = @Content(schema = @Schema(implementation = BonusRuleResponse.class))),
            @ApiResponse(responseCode = "400", description = "Payload ou parametro invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Conta nao encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public BonusRuleResponse upsertRule(
            @Parameter(description = "Id da conta da crianca", example = "1")
            @PathVariable @Positive(message = "accountId deve ser maior que zero.") Long accountId,
            @Valid @RequestBody UpdateBonusRuleRequest request
    ) {
        BonusRuleResult result = bonusRuleService.upsertRule(
                accountId,
                new UpdateBonusRuleCommand(
                        request.percentage(),
                        request.conditionType(),
                        request.baseType(),
                        request.active()
                )
        );

        return toResponse(result);
    }

    private BonusRuleResponse toResponse(BonusRuleResult result) {
        return new BonusRuleResponse(
                result.bonusRuleId(),
                result.accountId(),
                result.percentage(),
                result.conditionType(),
                result.baseType(),
                result.active(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}

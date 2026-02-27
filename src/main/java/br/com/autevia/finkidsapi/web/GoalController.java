package br.com.autevia.finkidsapi.web;

import br.com.autevia.finkidsapi.service.GoalService;
import br.com.autevia.finkidsapi.service.dto.goal.CreateGoalCommand;
import br.com.autevia.finkidsapi.service.dto.goal.GoalItemResult;
import br.com.autevia.finkidsapi.service.dto.goal.GoalListResult;
import br.com.autevia.finkidsapi.service.dto.goal.UpdateGoalCommand;
import br.com.autevia.finkidsapi.web.dto.goal.CreateGoalRequest;
import br.com.autevia.finkidsapi.web.dto.goal.GoalListResponse;
import br.com.autevia.finkidsapi.web.dto.goal.GoalResponse;
import br.com.autevia.finkidsapi.web.dto.goal.UpdateGoalRequest;
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
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/goals")
@Validated
@Tag(name = "Goals", description = "Gestao de metas de poupanca da conta da crianca.")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar meta", description = "Cria uma nova meta ativa para a conta informada.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Meta criada com sucesso",
                    content = @Content(schema = @Schema(implementation = GoalResponse.class))),
            @ApiResponse(responseCode = "400", description = "Payload invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Conta nao encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public GoalResponse create(@Valid @RequestBody CreateGoalRequest request) {
        GoalItemResult result = goalService.createGoal(
                new CreateGoalCommand(request.accountId(), request.name(), request.targetAmount())
        );
        return toResponse(result);
    }

    @GetMapping
    @Operation(summary = "Listar metas ativas", description = "Retorna as metas ativas da conta.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Metas listadas com sucesso",
                    content = @Content(schema = @Schema(implementation = GoalListResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parametro invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Conta nao encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public GoalListResponse list(
            @Parameter(description = "Id da conta da crianca", example = "1")
            @RequestParam @Positive(message = "accountId deve ser maior que zero.") Long accountId
    ) {
        GoalListResult result = goalService.listGoals(accountId);
        List<GoalResponse> goals = result.goals().stream()
                .map(this::toResponse)
                .toList();

        return new GoalListResponse(result.accountId(), goals);
    }

    @PutMapping("/{goalId}")
    @Operation(summary = "Atualizar meta", description = "Atualiza nome e valor alvo da meta ativa.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Meta atualizada com sucesso",
                    content = @Content(schema = @Schema(implementation = GoalResponse.class))),
            @ApiResponse(responseCode = "400", description = "Payload ou parametro invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Conta ou meta nao encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public GoalResponse update(
            @Parameter(description = "Id da meta", example = "11")
            @PathVariable @Positive(message = "goalId deve ser maior que zero.") Long goalId,
            @Valid @RequestBody UpdateGoalRequest request
    ) {
        GoalItemResult result = goalService.updateGoal(
                goalId,
                new UpdateGoalCommand(request.accountId(), request.name(), request.targetAmount())
        );
        return toResponse(result);
    }

    @DeleteMapping("/{goalId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remover meta", description = "Realiza exclusao logica de uma meta ativa.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Meta removida com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parametro invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Conta ou meta nao encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public void delete(
            @Parameter(description = "Id da meta", example = "11")
            @PathVariable @Positive(message = "goalId deve ser maior que zero.") Long goalId,
            @Parameter(description = "Id da conta da crianca", example = "1")
            @RequestParam @Positive(message = "accountId deve ser maior que zero.") Long accountId
    ) {
        goalService.deleteGoal(goalId, accountId);
    }

    private GoalResponse toResponse(GoalItemResult item) {
        return new GoalResponse(
                item.goalId(),
                item.accountId(),
                item.name(),
                item.targetAmount(),
                item.active(),
                item.createdAt(),
                item.updatedAt()
        );
    }
}

package br.com.autevia.finkidsapi.web;

import br.com.autevia.finkidsapi.service.AccountUserLinkService;
import br.com.autevia.finkidsapi.service.dto.accountuser.AccountUserLinkListResult;
import br.com.autevia.finkidsapi.service.dto.accountuser.AccountUserLinkResult;
import br.com.autevia.finkidsapi.web.dto.accountuser.AccountUserLinkListResponse;
import br.com.autevia.finkidsapi.web.dto.accountuser.AccountUserLinkResponse;
import br.com.autevia.finkidsapi.web.dto.accountuser.UpsertAccountUserLinkRequest;
import br.com.autevia.finkidsapi.web.error.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
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
@SecurityRequirement(name = "UserBearerAuth")
@Tag(name = "Account User Links", description = "Gestao administrativa de vinculos usuario-conta.")
public class AccountUserLinkController {

    private final AccountUserLinkService accountUserLinkService;

    public AccountUserLinkController(AccountUserLinkService accountUserLinkService) {
        this.accountUserLinkService = accountUserLinkService;
    }

    @GetMapping("/{accountId}/user-links")
    @Operation(summary = "Listar vinculos de usuarios por conta", description = "Retorna os usuarios vinculados e seus perfis na conta.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vinculos retornados com sucesso",
                    content = @Content(schema = @Schema(implementation = AccountUserLinkListResponse.class))),
            @ApiResponse(responseCode = "400", description = "Parametro invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Conta nao encontrada",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("@accountAuthorization.canWrite(#accountId)")
    public AccountUserLinkListResponse list(
            @Parameter(description = "Id da conta da crianca", example = "1")
            @PathVariable @Positive(message = "accountId deve ser maior que zero.") Long accountId
    ) {
        AccountUserLinkListResult result = accountUserLinkService.listLinks(accountId);
        List<AccountUserLinkResponse> links = result.links().stream()
                .map(this::toResponse)
                .toList();
        return new AccountUserLinkListResponse(result.accountId(), links);
    }

    @PutMapping("/{accountId}/user-links/{userId}")
    @Operation(summary = "Criar ou atualizar vinculo usuario-conta", description = "Cria o vinculo quando nao existe ou atualiza o profileRole quando ja existente.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vinculo salvo com sucesso",
                    content = @Content(schema = @Schema(implementation = AccountUserLinkResponse.class))),
            @ApiResponse(responseCode = "400", description = "Payload ou parametro invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Conta ou usuario nao encontrado",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PreAuthorize("@accountAuthorization.canWrite(#accountId)")
    public AccountUserLinkResponse upsert(
            @Parameter(description = "Id da conta da crianca", example = "1")
            @PathVariable @Positive(message = "accountId deve ser maior que zero.") Long accountId,
            @Parameter(description = "Id do usuario a ser vinculado", example = "2")
            @PathVariable @Positive(message = "userId deve ser maior que zero.") Long userId,
            @Valid @RequestBody UpsertAccountUserLinkRequest request
    ) {
        AccountUserLinkResult result = accountUserLinkService.upsertLink(accountId, userId, request.profileRole());
        return toResponse(result);
    }

    private AccountUserLinkResponse toResponse(AccountUserLinkResult result) {
        return new AccountUserLinkResponse(
                result.linkId(),
                result.accountId(),
                result.userId(),
                result.userFullName(),
                result.userEmail(),
                result.profileRole(),
                result.linkedAt()
        );
    }
}

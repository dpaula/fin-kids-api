package br.com.autevia.finkidsapi.web;

import br.com.autevia.finkidsapi.service.UserSessionService;
import br.com.autevia.finkidsapi.service.dto.user.UserAccountContextResult;
import br.com.autevia.finkidsapi.service.dto.user.UserSessionResult;
import br.com.autevia.finkidsapi.web.dto.user.UserAccountContextResponse;
import br.com.autevia.finkidsapi.web.dto.user.UserSessionResponse;
import br.com.autevia.finkidsapi.web.error.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@SecurityRequirement(name = "UserBearerAuth")
@Tag(name = "Users", description = "Contexto de sessao e dados do usuario autenticado.")
public class UserSessionController {

    private final UserSessionService userSessionService;

    public UserSessionController(UserSessionService userSessionService) {
        this.userSessionService = userSessionService;
    }

    @GetMapping("/me")
    @Operation(summary = "Consultar contexto da sessao atual", description = "Retorna usuario autenticado e contas vinculadas.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contexto retornado com sucesso",
                    content = @Content(schema = @Schema(implementation = UserSessionResponse.class))),
            @ApiResponse(responseCode = "401", description = "Token JWT ausente/invalido",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuario sem cadastro ou sem vinculo de conta",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public UserSessionResponse getCurrentUserSession() {
        UserSessionResult result = userSessionService.getCurrentUserSession();
        List<UserAccountContextResponse> accounts = result.accounts().stream()
                .map(this::toResponse)
                .toList();

        return new UserSessionResponse(
                result.userId(),
                result.fullName(),
                result.email(),
                result.globalRole(),
                accounts
        );
    }

    private UserAccountContextResponse toResponse(UserAccountContextResult item) {
        return new UserAccountContextResponse(item.accountId(), item.childName(), item.profileRole());
    }
}

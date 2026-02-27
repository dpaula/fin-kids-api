package br.com.autevia.finkidsapi.web.dto.user;

import br.com.autevia.finkidsapi.domain.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Contexto da sessao do usuario autenticado no WebApp.")
public record UserSessionResponse(
        @Schema(description = "Id interno do usuario", example = "10")
        Long userId,
        @Schema(description = "Nome completo do usuario", example = "Maria Silva")
        String fullName,
        @Schema(description = "Email autenticado no login", example = "maria@email.com")
        String email,
        @Schema(description = "Role global do usuario no sistema", example = "PARENT")
        UserRole globalRole,
        @Schema(description = "Contas vinculadas ao usuario com perfil de acesso.")
        List<UserAccountContextResponse> accounts
) {
}

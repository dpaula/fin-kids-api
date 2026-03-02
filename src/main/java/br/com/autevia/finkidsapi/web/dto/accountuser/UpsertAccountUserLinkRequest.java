package br.com.autevia.finkidsapi.web.dto.accountuser;

import br.com.autevia.finkidsapi.domain.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Payload para criar/atualizar vinculo de usuario em conta.")
public record UpsertAccountUserLinkRequest(
        @NotNull(message = "profileRole e obrigatorio.")
        @Schema(description = "Perfil do usuario na conta.", example = "PARENT")
        UserRole profileRole
) {
}

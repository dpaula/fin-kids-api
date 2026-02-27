package br.com.autevia.finkidsapi.web.dto.user;

import br.com.autevia.finkidsapi.domain.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Contexto de acesso do usuario em uma conta vinculada.")
public record UserAccountContextResponse(
        @Schema(description = "Id da conta da crianca", example = "1")
        Long accountId,
        @Schema(description = "Nome da crianca da conta", example = "Lucas")
        String childName,
        @Schema(description = "Perfil do usuario nesta conta", example = "PARENT")
        UserRole profileRole
) {
}

package br.com.autevia.finkidsapi.web.dto.accountuser;

import br.com.autevia.finkidsapi.domain.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Vinculo de usuario com uma conta da crianca.")
public record AccountUserLinkResponse(
        @Schema(description = "Id do vinculo", example = "10")
        Long linkId,
        @Schema(description = "Id da conta", example = "1")
        Long accountId,
        @Schema(description = "Id do usuario vinculado", example = "2")
        Long userId,
        @Schema(description = "Nome completo do usuario", example = "Maria Silva")
        String userFullName,
        @Schema(description = "Email do usuario", example = "maria@email.com")
        String userEmail,
        @Schema(description = "Perfil do usuario na conta", example = "PARENT")
        UserRole profileRole,
        @Schema(description = "Data/hora em que o vinculo foi criado", example = "2026-03-02T12:00:00Z")
        Instant linkedAt
) {
}

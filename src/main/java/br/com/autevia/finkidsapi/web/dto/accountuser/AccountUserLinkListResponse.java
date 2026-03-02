package br.com.autevia.finkidsapi.web.dto.accountuser;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Lista de vinculos de usuarios para uma conta.")
public record AccountUserLinkListResponse(
        @Schema(description = "Id da conta", example = "1")
        Long accountId,
        @Schema(description = "Lista de usuarios vinculados.")
        List<AccountUserLinkResponse> links
) {
}

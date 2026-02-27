package br.com.autevia.finkidsapi.web.dto.goal;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Resposta de listagem de metas ativas.")
public record GoalListResponse(
        @Schema(description = "Id da conta da crianca", example = "1")
        Long accountId,
        @Schema(description = "Metas ativas da conta")
        List<GoalResponse> goals
) {
}

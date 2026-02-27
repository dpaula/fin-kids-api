package br.com.autevia.finkidsapi.web.dto.goal;

import java.util.List;

public record GoalListResponse(
        Long accountId,
        List<GoalResponse> goals
) {
}

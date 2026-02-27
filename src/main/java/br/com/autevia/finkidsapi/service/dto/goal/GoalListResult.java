package br.com.autevia.finkidsapi.service.dto.goal;

import java.util.List;

public record GoalListResult(
        Long accountId,
        List<GoalItemResult> goals
) {
}

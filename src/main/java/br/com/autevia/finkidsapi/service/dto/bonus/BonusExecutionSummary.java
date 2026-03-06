package br.com.autevia.finkidsapi.service.dto.bonus;

public record BonusExecutionSummary(
        int totalRules,
        int eligibleRules,
        int appliedBonuses,
        int skippedRules,
        int failedRules
) {
}

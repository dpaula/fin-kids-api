package br.com.autevia.finkidsapi.service.dto.view;

import java.math.BigDecimal;
import java.util.List;

public record ParentAccountViewResult(
        Long accountId,
        String childName,
        String currencyCode,
        BigDecimal currentBalance,
        ParentMonthlySummaryResult monthlySummary,
        ParentBonusRuleResult bonusRule,
        List<GoalProgressItemResult> goals,
        List<ParentTransactionItemResult> recentTransactions
) {
}

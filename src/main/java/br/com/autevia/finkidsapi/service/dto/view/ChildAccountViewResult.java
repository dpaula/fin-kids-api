package br.com.autevia.finkidsapi.service.dto.view;

import java.math.BigDecimal;
import java.util.List;

public record ChildAccountViewResult(
        Long accountId,
        String childName,
        String currencyCode,
        BigDecimal currentBalance,
        List<GoalProgressItemResult> goals,
        List<ChildTransactionItemResult> recentTransactions
) {
}

package br.com.autevia.finkidsapi.web;

import br.com.autevia.finkidsapi.service.AccountSummaryService;
import br.com.autevia.finkidsapi.service.dto.account.AccountBalanceResult;
import br.com.autevia.finkidsapi.service.dto.account.MonthlySummaryByOriginResult;
import br.com.autevia.finkidsapi.service.dto.account.MonthlySummaryByTypeResult;
import br.com.autevia.finkidsapi.service.dto.account.MonthlySummaryResult;
import br.com.autevia.finkidsapi.web.dto.account.AccountBalanceResponse;
import br.com.autevia.finkidsapi.web.dto.account.MonthlySummaryByOriginResponse;
import br.com.autevia.finkidsapi.web.dto.account.MonthlySummaryByTypeResponse;
import br.com.autevia.finkidsapi.web.dto.account.MonthlySummaryResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
@Validated
public class AccountSummaryController {

    private final AccountSummaryService accountSummaryService;

    public AccountSummaryController(AccountSummaryService accountSummaryService) {
        this.accountSummaryService = accountSummaryService;
    }

    @GetMapping("/{accountId}/balance")
    public AccountBalanceResponse getBalance(
            @PathVariable @Positive(message = "accountId deve ser maior que zero.") Long accountId
    ) {
        AccountBalanceResult result = accountSummaryService.getBalance(accountId);
        return new AccountBalanceResponse(result.accountId(), result.currentBalance());
    }

    @GetMapping("/{accountId}/monthly-summary")
    public MonthlySummaryResponse getMonthlySummary(
            @PathVariable @Positive(message = "accountId deve ser maior que zero.") Long accountId,
            @RequestParam @Min(value = 2000, message = "year deve estar entre 2000 e 2100.")
            @Max(value = 2100, message = "year deve estar entre 2000 e 2100.") int year,
            @RequestParam @Min(value = 1, message = "month deve estar entre 1 e 12.")
            @Max(value = 12, message = "month deve estar entre 1 e 12.") int month
    ) {
        MonthlySummaryResult result = accountSummaryService.getMonthlySummary(accountId, year, month);

        List<MonthlySummaryByTypeResponse> typeItems = result.totalsByType().stream()
                .map(this::toTypeResponse)
                .toList();

        List<MonthlySummaryByOriginResponse> originItems = result.totalsByOrigin().stream()
                .map(this::toOriginResponse)
                .toList();

        return new MonthlySummaryResponse(
                result.accountId(),
                result.year(),
                result.month(),
                result.periodStart(),
                result.periodEnd(),
                result.currentBalance(),
                result.totalDeposits(),
                result.totalWithdrawals(),
                result.netChange(),
                typeItems,
                originItems
        );
    }

    private MonthlySummaryByTypeResponse toTypeResponse(MonthlySummaryByTypeResult item) {
        return new MonthlySummaryByTypeResponse(item.type(), item.total());
    }

    private MonthlySummaryByOriginResponse toOriginResponse(MonthlySummaryByOriginResult item) {
        return new MonthlySummaryByOriginResponse(item.origin(), item.total());
    }
}

package br.com.autevia.finkidsapi.web;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.autevia.finkidsapi.domain.enums.TransactionOrigin;
import br.com.autevia.finkidsapi.domain.enums.TransactionType;
import br.com.autevia.finkidsapi.domain.exception.ResourceNotFoundException;
import br.com.autevia.finkidsapi.security.AccountAuthorizationService;
import br.com.autevia.finkidsapi.service.AccountSummaryService;
import br.com.autevia.finkidsapi.service.dto.account.AccountBalanceResult;
import br.com.autevia.finkidsapi.service.dto.account.MonthlySummaryByOriginResult;
import br.com.autevia.finkidsapi.service.dto.account.MonthlySummaryByTypeResult;
import br.com.autevia.finkidsapi.service.dto.account.MonthlySummaryResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = AccountSummaryController.class,
        excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class
)
@Import(ApiExceptionHandler.class)
class AccountSummaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountSummaryService accountSummaryService;

    @MockitoBean
    private AccountAuthorizationService accountAuthorizationService;

    @BeforeEach
    void setupAuthorization() {
        when(accountAuthorizationService.canRead(anyLong())).thenReturn(true);
        when(accountAuthorizationService.canWrite(anyLong())).thenReturn(true);
    }

    @Test
    void shouldReturnAccountBalance() throws Exception {
        when(accountSummaryService.getBalance(1L))
                .thenReturn(new AccountBalanceResult(1L, new BigDecimal("123.45")));

        mockMvc.perform(get("/api/v1/accounts/1/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(1))
                .andExpect(jsonPath("$.currentBalance").value(123.45));
    }

    @Test
    void shouldReturnMonthlySummary() throws Exception {
        MonthlySummaryResult summary = new MonthlySummaryResult(
                1L,
                2026,
                2,
                Instant.parse("2026-02-01T00:00:00Z"),
                Instant.parse("2026-03-01T00:00:00Z"),
                new BigDecimal("180.00"),
                new BigDecimal("250.00"),
                new BigDecimal("70.00"),
                new BigDecimal("180.00"),
                List.of(
                        new MonthlySummaryByTypeResult(TransactionType.DEPOSIT, new BigDecimal("250.00")),
                        new MonthlySummaryByTypeResult(TransactionType.WITHDRAW, new BigDecimal("70.00"))
                ),
                List.of(
                        new MonthlySummaryByOriginResult(TransactionOrigin.MANUAL, new BigDecimal("200.00")),
                        new MonthlySummaryByOriginResult(TransactionOrigin.BONUS, new BigDecimal("50.00")),
                        new MonthlySummaryByOriginResult(TransactionOrigin.WHATSAPP, new BigDecimal("70.00"))
                )
        );

        when(accountSummaryService.getMonthlySummary(1L, 2026, 2)).thenReturn(summary);

        mockMvc.perform(get("/api/v1/accounts/1/monthly-summary")
                        .param("year", "2026")
                        .param("month", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(1))
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.month").value(2))
                .andExpect(jsonPath("$.totalDeposits").value(250.00))
                .andExpect(jsonPath("$.totalWithdrawals").value(70.00))
                .andExpect(jsonPath("$.netChange").value(180.00))
                .andExpect(jsonPath("$.totalsByType[0].type").exists())
                .andExpect(jsonPath("$.totalsByOrigin[0].origin").exists());
    }

    @Test
    void shouldReturnNotFoundWhenAccountDoesNotExist() throws Exception {
        when(accountSummaryService.getBalance(anyLong()))
                .thenThrow(new ResourceNotFoundException("Conta nao encontrada para id=99"));

        mockMvc.perform(get("/api/v1/accounts/99/balance"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Conta nao encontrada para id=99"));
    }

    @Test
    void shouldReturnBadRequestWhenMonthIsInvalid() throws Exception {
        when(accountSummaryService.getMonthlySummary(anyLong(), anyInt(), anyInt()))
                .thenThrow(new br.com.autevia.finkidsapi.domain.exception.ValidationException("month deve estar entre 1 e 12."));

        mockMvc.perform(get("/api/v1/accounts/1/monthly-summary")
                        .param("year", "2026")
                        .param("month", "13"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("month deve estar entre 1 e 12."));
    }
}

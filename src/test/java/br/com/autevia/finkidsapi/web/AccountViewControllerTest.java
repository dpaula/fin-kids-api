package br.com.autevia.finkidsapi.web;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.autevia.finkidsapi.domain.enums.BonusBaseType;
import br.com.autevia.finkidsapi.domain.enums.BonusConditionType;
import br.com.autevia.finkidsapi.domain.enums.TransactionOrigin;
import br.com.autevia.finkidsapi.domain.enums.TransactionType;
import br.com.autevia.finkidsapi.domain.exception.ResourceNotFoundException;
import br.com.autevia.finkidsapi.security.AccountAuthorizationService;
import br.com.autevia.finkidsapi.service.AccountViewService;
import br.com.autevia.finkidsapi.service.dto.view.ChildAccountViewResult;
import br.com.autevia.finkidsapi.service.dto.view.ChildTransactionItemResult;
import br.com.autevia.finkidsapi.service.dto.view.GoalProgressItemResult;
import br.com.autevia.finkidsapi.service.dto.view.ParentAccountViewResult;
import br.com.autevia.finkidsapi.service.dto.view.ParentBonusRuleResult;
import br.com.autevia.finkidsapi.service.dto.view.ParentMonthlySummaryByOriginResult;
import br.com.autevia.finkidsapi.service.dto.view.ParentMonthlySummaryByTypeResult;
import br.com.autevia.finkidsapi.service.dto.view.ParentMonthlySummaryResult;
import br.com.autevia.finkidsapi.service.dto.view.ParentTransactionItemResult;
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
        value = AccountViewController.class,
        excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class
)
@Import(ApiExceptionHandler.class)
class AccountViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountViewService accountViewService;

    @MockitoBean
    private AccountAuthorizationService accountAuthorizationService;

    @BeforeEach
    void setupAuthorization() {
        when(accountAuthorizationService.canRead(anyLong())).thenReturn(true);
        when(accountAuthorizationService.canWrite(anyLong())).thenReturn(true);
    }

    @Test
    void shouldReturnChildView() throws Exception {
        ChildAccountViewResult result = new ChildAccountViewResult(
                1L,
                "Lucas",
                "BRL",
                new BigDecimal("180.00"),
                List.of(
                        new GoalProgressItemResult(
                                11L,
                                "Bicicleta",
                                new BigDecimal("500.00"),
                                new BigDecimal("180.00"),
                                new BigDecimal("36.00"),
                                new BigDecimal("320.00"),
                                false
                        )
                ),
                List.of(
                        new ChildTransactionItemResult(
                                101L,
                                TransactionType.DEPOSIT,
                                new BigDecimal("100.00"),
                                "Mesada",
                                Instant.parse("2026-03-01T10:00:00Z")
                        )
                )
        );
        when(accountViewService.getChildView(1L, 10)).thenReturn(result);

        mockMvc.perform(get("/api/v1/accounts/1/child-view"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(1))
                .andExpect(jsonPath("$.currentBalance").value(180.00))
                .andExpect(jsonPath("$.goals[0].goalId").value(11))
                .andExpect(jsonPath("$.goals[0].progressPercent").value(36.00))
                .andExpect(jsonPath("$.recentTransactions[0].type").value("DEPOSIT"));
    }

    @Test
    void shouldReturnParentView() throws Exception {
        ParentAccountViewResult result = new ParentAccountViewResult(
                1L,
                "Lucas",
                "BRL",
                new BigDecimal("180.00"),
                new ParentMonthlySummaryResult(
                        2026,
                        3,
                        Instant.parse("2026-03-01T00:00:00Z"),
                        Instant.parse("2026-04-01T00:00:00Z"),
                        new BigDecimal("250.00"),
                        new BigDecimal("70.00"),
                        new BigDecimal("180.00"),
                        List.of(
                                new ParentMonthlySummaryByTypeResult(TransactionType.DEPOSIT, new BigDecimal("250.00")),
                                new ParentMonthlySummaryByTypeResult(TransactionType.WITHDRAW, new BigDecimal("70.00"))
                        ),
                        List.of(
                                new ParentMonthlySummaryByOriginResult(TransactionOrigin.MANUAL, new BigDecimal("250.00")),
                                new ParentMonthlySummaryByOriginResult(TransactionOrigin.WHATSAPP, new BigDecimal("70.00"))
                        )
                ),
                new ParentBonusRuleResult(
                        new BigDecimal("5.00"),
                        BonusConditionType.NO_WITHDRAWALS_IN_MONTH,
                        BonusBaseType.MONTHLY_DEPOSITS,
                        true
                ),
                List.of(
                        new GoalProgressItemResult(
                                11L,
                                "Bicicleta",
                                new BigDecimal("500.00"),
                                new BigDecimal("180.00"),
                                new BigDecimal("36.00"),
                                new BigDecimal("320.00"),
                                false
                        )
                ),
                List.of(
                        new ParentTransactionItemResult(
                                501L,
                                TransactionType.WITHDRAW,
                                TransactionOrigin.WHATSAPP,
                                new BigDecimal("70.00"),
                                "Compra",
                                "wa-media-1",
                                Instant.parse("2026-03-08T10:00:00Z")
                        )
                )
        );
        when(accountViewService.getParentView(1L, 2026, 3, 20)).thenReturn(result);

        mockMvc.perform(get("/api/v1/accounts/1/parent-view")
                        .param("year", "2026")
                        .param("month", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(1))
                .andExpect(jsonPath("$.monthlySummary.year").value(2026))
                .andExpect(jsonPath("$.monthlySummary.totalDeposits").value(250.00))
                .andExpect(jsonPath("$.bonusRule.percentage").value(5.00))
                .andExpect(jsonPath("$.recentTransactions[0].origin").value("WHATSAPP"));
    }

    @Test
    void shouldReturnNotFoundWhenAccountDoesNotExist() throws Exception {
        when(accountViewService.getChildView(anyLong(), anyInt()))
                .thenThrow(new ResourceNotFoundException("Conta nao encontrada para id=99"));

        mockMvc.perform(get("/api/v1/accounts/99/child-view"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Conta nao encontrada para id=99"));
    }

    @Test
    void shouldReturnBadRequestWhenRecentTransactionsLimitIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/1/child-view")
                        .param("recentTransactionsLimit", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void shouldReturnBadRequestWhenMonthIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/1/parent-view")
                        .param("year", "2026")
                        .param("month", "13"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }
}

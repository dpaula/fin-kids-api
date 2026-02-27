package br.com.autevia.finkidsapi.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.autevia.finkidsapi.domain.enums.BonusBaseType;
import br.com.autevia.finkidsapi.domain.enums.BonusConditionType;
import br.com.autevia.finkidsapi.domain.exception.ResourceNotFoundException;
import br.com.autevia.finkidsapi.security.AccountAuthorizationService;
import br.com.autevia.finkidsapi.service.BonusRuleService;
import br.com.autevia.finkidsapi.service.dto.bonus.BonusRuleResult;
import jakarta.validation.ConstraintViolationException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
        value = BonusRuleController.class,
        excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class
)
@Import(ApiExceptionHandler.class)
class BonusRuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BonusRuleService bonusRuleService;

    @MockitoBean
    private AccountAuthorizationService accountAuthorizationService;

    @BeforeEach
    void setupAuthorization() {
        when(accountAuthorizationService.canRead(anyLong())).thenReturn(true);
        when(accountAuthorizationService.canWrite(anyLong())).thenReturn(true);
    }

    @Test
    void shouldGetBonusRule() throws Exception {
        when(bonusRuleService.getRule(1L)).thenReturn(result("8.50", true));

        mockMvc.perform(get("/api/v1/accounts/1/bonus-rule"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bonusRuleId").value(15))
                .andExpect(jsonPath("$.accountId").value(1))
                .andExpect(jsonPath("$.percentage").value(8.50))
                .andExpect(jsonPath("$.conditionType").value("NO_WITHDRAWALS_IN_MONTH"))
                .andExpect(jsonPath("$.baseType").value("MONTHLY_DEPOSITS"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldUpsertBonusRule() throws Exception {
        when(bonusRuleService.upsertRule(org.mockito.ArgumentMatchers.eq(1L), any()))
                .thenReturn(result("10.00", true));

        String payload = """
                {
                  "percentage": 10.00,
                  "conditionType": "NO_WITHDRAWALS_IN_MONTH",
                  "baseType": "MONTHLY_DEPOSITS",
                  "active": true
                }
                """;

        mockMvc.perform(put("/api/v1/accounts/1/bonus-rule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bonusRuleId").value(15))
                .andExpect(jsonPath("$.percentage").value(10.00))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldReturnNotFoundWhenBonusRuleDoesNotExist() throws Exception {
        when(bonusRuleService.getRule(99L))
                .thenThrow(new ResourceNotFoundException("Regra de bonus nao encontrada para accountId=99"));

        mockMvc.perform(get("/api/v1/accounts/99/bonus-rule"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Regra de bonus nao encontrada para accountId=99"));
    }

    @Test
    void shouldReturnBadRequestWhenPayloadIsInvalid() throws Exception {
        String payload = """
                {
                  "percentage": 0,
                  "conditionType": null,
                  "baseType": "MONTHLY_DEPOSITS"
                }
                """;

        mockMvc.perform(put("/api/v1/accounts/1/bonus-rule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void shouldReturnBadRequestWhenAccountIdIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/0/bonus-rule"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void shouldReturnBadRequestWhenConstraintViolationOccurs() throws Exception {
        when(bonusRuleService.getRule(1L))
                .thenThrow(new ConstraintViolationException("Dados de entrada invalidos.", Set.of()));

        mockMvc.perform(get("/api/v1/accounts/1/bonus-rule"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dados de entrada invalidos."));
    }

    private BonusRuleResult result(String percentage, boolean active) {
        return new BonusRuleResult(
                15L,
                1L,
                new BigDecimal(percentage),
                BonusConditionType.NO_WITHDRAWALS_IN_MONTH,
                BonusBaseType.MONTHLY_DEPOSITS,
                active,
                Instant.parse("2026-02-27T14:00:00Z"),
                Instant.parse("2026-02-27T14:00:00Z")
        );
    }
}

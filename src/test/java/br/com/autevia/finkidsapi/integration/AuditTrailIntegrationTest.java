package br.com.autevia.finkidsapi.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.autevia.finkidsapi.domain.entity.Account;
import br.com.autevia.finkidsapi.domain.entity.AccountUser;
import br.com.autevia.finkidsapi.domain.entity.AppUser;
import br.com.autevia.finkidsapi.domain.entity.AuditEvent;
import br.com.autevia.finkidsapi.domain.enums.AuditActionType;
import br.com.autevia.finkidsapi.domain.enums.AuditResourceType;
import br.com.autevia.finkidsapi.domain.enums.UserRole;
import br.com.autevia.finkidsapi.repository.AccountRepository;
import br.com.autevia.finkidsapi.repository.AccountUserRepository;
import br.com.autevia.finkidsapi.repository.AppUserRepository;
import br.com.autevia.finkidsapi.repository.AuditEventRepository;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "app.security.automation.token=test-automation-token")
@Transactional
class AuditTrailIntegrationTest {

    private static final String VALID_AUTOMATION_AUTHORIZATION = "Bearer test-automation-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private AccountUserRepository accountUserRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Test
    void shouldAuditManualTransactionCreation() throws Exception {
        Account account = accountRepository.save(new Account("Luca", "BRL"));
        String parentEmail = "parent.audit.tx@test.com";
        linkAccountUser(account, parentEmail, UserRole.PARENT);

        mockMvc.perform(post("/api/v1/transactions")
                        .with(jwt().jwt(jwt -> jwt.claim("email", parentEmail)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountId": %d,
                                  "type": "DEPOSIT",
                                  "origin": "MANUAL",
                                  "amount": 100.00,
                                  "description": "Mesada"
                                }
                                """.formatted(account.getId())))
                .andExpect(status().isCreated());

        List<AuditEvent> events = auditEventRepository.findByAccountIdOrderByCreatedAtAsc(account.getId());
        assertThat(events).hasSize(1);
        AuditEvent event = events.getFirst();
        assertThat(event.getActionType()).isEqualTo(AuditActionType.TRANSACTION_MANUAL_CREATED);
        assertThat(event.getResourceType()).isEqualTo(AuditResourceType.TRANSACTION);
        assertThat(event.getActorEmail()).isEqualTo(parentEmail);
        assertThat(event.getPayloadSummary()).contains("origin=MANUAL");
    }

    @Test
    void shouldNotAuditAutomationTransactionEndpoint() throws Exception {
        Account account = accountRepository.save(new Account("Theo", "BRL"));

        mockMvc.perform(post("/api/v1/automation/transactions")
                        .header(HttpHeaders.AUTHORIZATION, VALID_AUTOMATION_AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountId": %d,
                                  "type": "DEPOSIT",
                                  "amount": 90.00,
                                  "description": "Deposito via comprovante"
                                }
                                """.formatted(account.getId())))
                .andExpect(status().isCreated());

        assertThat(auditEventRepository.findByAccountIdOrderByCreatedAtAsc(account.getId())).isEmpty();
    }

    @Test
    void shouldAuditGoalAndBonusRuleSensitiveChanges() throws Exception {
        Account account = accountRepository.save(new Account("Nina", "BRL"));
        String parentEmail = "parent.audit.goal.bonus@test.com";
        linkAccountUser(account, parentEmail, UserRole.PARENT);

        MvcResult createGoalResult = mockMvc.perform(post("/api/v1/goals")
                        .with(jwt().jwt(jwt -> jwt.claim("email", parentEmail)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountId": %d,
                                  "name": "Bicicleta",
                                  "targetAmount": 500.00
                                }
                                """.formatted(account.getId())))
                .andExpect(status().isCreated())
                .andReturn();

        Number goalIdValue = JsonPath.read(createGoalResult.getResponse().getContentAsString(), "$.goalId");
        long goalId = goalIdValue.longValue();

        mockMvc.perform(put("/api/v1/goals/{goalId}", goalId)
                        .with(jwt().jwt(jwt -> jwt.claim("email", parentEmail)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountId": %d,
                                  "name": "Notebook",
                                  "targetAmount": 3200.00
                                }
                                """.formatted(account.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/goals/{goalId}", goalId)
                        .with(jwt().jwt(jwt -> jwt.claim("email", parentEmail)))
                        .param("accountId", String.valueOf(account.getId())))
                .andExpect(status().isNoContent());

        mockMvc.perform(put("/api/v1/accounts/{accountId}/bonus-rule", account.getId())
                        .with(jwt().jwt(jwt -> jwt.claim("email", parentEmail)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "percentage": 10.00,
                                  "conditionType": "NO_WITHDRAWALS_IN_MONTH",
                                  "baseType": "MONTHLY_DEPOSITS",
                                  "active": true
                                }
                                """))
                .andExpect(status().isOk());

        List<AuditEvent> events = auditEventRepository.findByAccountIdOrderByCreatedAtAsc(account.getId());
        assertThat(events).hasSize(4);
        assertThat(events).extracting(AuditEvent::getActionType).containsExactly(
                AuditActionType.GOAL_CREATED,
                AuditActionType.GOAL_UPDATED,
                AuditActionType.GOAL_DELETED,
                AuditActionType.BONUS_RULE_UPSERTED
        );
    }

    private void linkAccountUser(Account account, String email, UserRole role) {
        AppUser user = appUserRepository.save(new AppUser("User " + role.name(), email, "google-" + email, role));
        accountUserRepository.save(new AccountUser(account, user, role));
    }
}

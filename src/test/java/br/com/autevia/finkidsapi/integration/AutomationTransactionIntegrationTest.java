package br.com.autevia.finkidsapi.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.autevia.finkidsapi.domain.entity.Account;
import br.com.autevia.finkidsapi.domain.entity.AccountTransaction;
import br.com.autevia.finkidsapi.domain.enums.TransactionOrigin;
import br.com.autevia.finkidsapi.repository.AccountRepository;
import br.com.autevia.finkidsapi.repository.AccountTransactionRepository;
import java.time.Instant;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestPropertySource(properties = "app.security.automation.token=test-automation-token")
@Transactional
class AutomationTransactionIntegrationTest {

    private static final String VALID_AUTHORIZATION = "Bearer test-automation-token";
    private static final Instant RANGE_START = Instant.parse("2000-01-01T00:00:00Z");
    private static final Instant RANGE_END = Instant.parse("2100-01-01T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountTransactionRepository accountTransactionRepository;

    @Test
    void shouldCreateTransactionViaAutomationAndPersistWhatsappOrigin() throws Exception {
        Account account = accountRepository.save(new Account("Noah", "BRL"));

        mockMvc.perform(post("/api/v1/automation/transactions")
                        .header(HttpHeaders.AUTHORIZATION, VALID_AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountId": %d,
                                  "type": "DEPOSIT",
                                  "amount": 120.00,
                                  "description": "Deposito lido do comprovante",
                                  "evidenceReference": "wa-media-001",
                                  "occurredAt": "2026-03-05T10:00:00Z"
                                }
                                """.formatted(account.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").isNumber())
                .andExpect(jsonPath("$.updatedBalance").value(120.00));

        List<AccountTransaction> transactions = listAccountTransactions(account.getId());
        assertThat(transactions).hasSize(1);
        assertThat(transactions.getFirst().getOrigin()).isEqualTo(TransactionOrigin.WHATSAPP);
        assertThat(transactions.getFirst().getEvidenceReference()).isEqualTo("wa-media-001");
    }

    @Test
    void shouldRejectWhenAuthorizationHeaderIsMissing() throws Exception {
        Account account = accountRepository.save(new Account("Lia", "BRL"));

        mockMvc.perform(post("/api/v1/automation/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountId": %d,
                                  "type": "DEPOSIT",
                                  "amount": 50.00,
                                  "description": "Deposito"
                                }
                                """.formatted(account.getId())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token de automacao ausente ou invalido."));
    }

    @Test
    void shouldRejectWhenAuthorizationTokenIsInvalid() throws Exception {
        Account account = accountRepository.save(new Account("Cora", "BRL"));

        mockMvc.perform(post("/api/v1/automation/transactions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountId": %d,
                                  "type": "DEPOSIT",
                                  "amount": 50.00,
                                  "description": "Deposito"
                                }
                                """.formatted(account.getId())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token de automacao ausente ou invalido."));
    }

    @Test
    void shouldReturnUnprocessableWhenAutomationWithdrawWouldMakeNegativeBalance() throws Exception {
        Account account = accountRepository.save(new Account("Bia", "BRL"));

        mockMvc.perform(post("/api/v1/automation/transactions")
                        .header(HttpHeaders.AUTHORIZATION, VALID_AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountId": %d,
                                  "type": "WITHDRAW",
                                  "amount": 30.00,
                                  "description": "Compra via nota"
                                }
                                """.formatted(account.getId())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Saldo insuficiente para realizar saque."));

        assertThat(listAccountTransactions(account.getId())).isEmpty();
    }

    @Test
    void shouldReturnBadRequestWhenPayloadIsInvalid() throws Exception {
        Account account = accountRepository.save(new Account("Maya", "BRL"));

        mockMvc.perform(post("/api/v1/automation/transactions")
                        .header(HttpHeaders.AUTHORIZATION, VALID_AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountId": %d,
                                  "type": "DEPOSIT",
                                  "amount": 0,
                                  "description": ""
                                }
                                """.formatted(account.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());

        assertThat(listAccountTransactions(account.getId())).isEmpty();
    }

    @Test
    void shouldReturnNotFoundWhenAccountDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/v1/automation/transactions")
                        .header(HttpHeaders.AUTHORIZATION, VALID_AUTHORIZATION)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountId": 999999999,
                                  "type": "DEPOSIT",
                                  "amount": 50.00,
                                  "description": "Deposito"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Conta nao encontrada para id=999999999"));
    }

    private List<AccountTransaction> listAccountTransactions(Long accountId) {
        return accountTransactionRepository.findByAccountIdAndOccurredAtBetweenOrderByOccurredAtDesc(
                accountId,
                RANGE_START,
                RANGE_END
        );
    }
}

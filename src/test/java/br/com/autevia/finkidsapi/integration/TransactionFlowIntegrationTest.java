package br.com.autevia.finkidsapi.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.autevia.finkidsapi.domain.entity.Account;
import br.com.autevia.finkidsapi.domain.entity.AccountUser;
import br.com.autevia.finkidsapi.domain.entity.AccountTransaction;
import br.com.autevia.finkidsapi.domain.entity.AppUser;
import br.com.autevia.finkidsapi.domain.enums.UserRole;
import br.com.autevia.finkidsapi.repository.AccountUserRepository;
import br.com.autevia.finkidsapi.repository.AccountRepository;
import br.com.autevia.finkidsapi.repository.AccountTransactionRepository;
import br.com.autevia.finkidsapi.repository.AppUserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class TransactionFlowIntegrationTest {

    private static final Instant RANGE_START = Instant.parse("2000-01-01T00:00:00Z");
    private static final Instant RANGE_END = Instant.parse("2100-01-01T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountTransactionRepository accountTransactionRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private AccountUserRepository accountUserRepository;

    @Test
    void shouldCreateDepositAndPersistTransaction() throws Exception {
        Account account = accountRepository.save(new Account("Lucas", "BRL"));
        String parentEmail = "parent.deposit@test.com";
        linkAccountUser(account, parentEmail, UserRole.PARENT);

        String payload = """
                {
                  "accountId": %d,
                  "type": "DEPOSIT",
                  "origin": "MANUAL",
                  "amount": 100.00,
                  "description": "Mesada",
                  "occurredAt": "2026-02-01T10:00:00Z"
                }
                """.formatted(account.getId());

        mockMvc.perform(post("/api/v1/transactions")
                        .with(jwt().jwt(jwt -> jwt.claim("email", parentEmail)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").isNumber())
                .andExpect(jsonPath("$.updatedBalance").value(100.00));

        List<AccountTransaction> transactions = listAccountTransactions(account.getId());
        assertThat(transactions).hasSize(1);
        assertThat(transactions.getFirst().getDescription()).isEqualTo("Mesada");
        assertThat(transactions.getFirst().getAmount()).isEqualByComparingTo("100.00");
        assertThat(accountTransactionRepository.calculateBalanceByAccountId(account.getId()))
                .isEqualByComparingTo("100.00");
    }

    @Test
    void shouldCreateWithdrawWhenBalanceIsSufficient() throws Exception {
        Account account = accountRepository.save(new Account("Ana", "BRL"));
        String parentEmail = "parent.withdraw@test.com";
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
                                  "description": "Mesada",
                                  "occurredAt": "2026-02-05T10:00:00Z"
                                }
                                """.formatted(account.getId())))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/transactions")
                        .with(jwt().jwt(jwt -> jwt.claim("email", parentEmail)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountId": %d,
                                  "type": "WITHDRAW",
                                  "origin": "MANUAL",
                                  "amount": 60.00,
                                  "description": "Compra",
                                  "occurredAt": "2026-02-06T10:00:00Z"
                                }
                                """.formatted(account.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.updatedBalance").value(40.00));

        List<AccountTransaction> transactions = listAccountTransactions(account.getId());
        assertThat(transactions).hasSize(2);
        assertThat(accountTransactionRepository.calculateBalanceByAccountId(account.getId()))
                .isEqualByComparingTo("40.00");
    }

    @Test
    void shouldRejectWithdrawWhenBalanceIsInsufficientAndNotPersistTransaction() throws Exception {
        Account account = accountRepository.save(new Account("Bia", "BRL"));
        String parentEmail = "parent.insufficient@test.com";
        linkAccountUser(account, parentEmail, UserRole.PARENT);

        mockMvc.perform(post("/api/v1/transactions")
                        .with(jwt().jwt(jwt -> jwt.claim("email", parentEmail)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountId": %d,
                                  "type": "WITHDRAW",
                                  "origin": "MANUAL",
                                  "amount": 30.00,
                                  "description": "Compra",
                                  "occurredAt": "2026-02-10T10:00:00Z"
                                }
                                """.formatted(account.getId())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Saldo insuficiente para realizar saque."));

        assertThat(listAccountTransactions(account.getId())).isEmpty();
        assertThat(accountTransactionRepository.calculateBalanceByAccountId(account.getId()))
                .isEqualByComparingTo("0.00");
    }

    @Test
    void shouldReturnBadRequestForInvalidPayload() throws Exception {
        Account account = accountRepository.save(new Account("Nina", "BRL"));
        String parentEmail = "parent.badrequest@test.com";
        linkAccountUser(account, parentEmail, UserRole.PARENT);

        mockMvc.perform(post("/api/v1/transactions")
                        .with(jwt().jwt(jwt -> jwt.claim("email", parentEmail)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "accountId": %d,
                                  "type": "DEPOSIT",
                                  "origin": "MANUAL",
                                  "amount": 0,
                                  "description": ""
                                }
                                """.formatted(account.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());

        assertThat(listAccountTransactions(account.getId())).isEmpty();
    }

    @Test
    void shouldReturnForbiddenWhenAccountIsNotLinkedToAuthenticatedUser() throws Exception {
        String parentEmail = "parent.notfound@test.com";
        appUserRepository.save(new AppUser("Parent", parentEmail, "google-parent-notfound", UserRole.PARENT));

        mockMvc.perform(post("/api/v1/transactions")
                        .with(jwt().jwt(jwt -> jwt.claim("email", parentEmail)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                                  "accountId": 999999999,
                                  "type": "DEPOSIT",
                                  "origin": "MANUAL",
                                  "amount": 50.00,
                                  "description": "Mesada"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Acesso negado para este recurso."));
    }

    private void linkAccountUser(Account account, String email, UserRole role) {
        AppUser user = appUserRepository.save(
                new AppUser("User " + role.name(), email, "google-" + email, role)
        );
        accountUserRepository.save(new AccountUser(account, user, role));
    }

    private List<AccountTransaction> listAccountTransactions(Long accountId) {
        return accountTransactionRepository.findByAccountIdAndOccurredAtBetweenOrderByOccurredAtDesc(
                accountId,
                RANGE_START,
                RANGE_END
        );
    }
}

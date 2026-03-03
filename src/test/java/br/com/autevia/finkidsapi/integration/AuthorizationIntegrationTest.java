package br.com.autevia.finkidsapi.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.autevia.finkidsapi.domain.entity.Account;
import br.com.autevia.finkidsapi.domain.entity.AccountUser;
import br.com.autevia.finkidsapi.domain.entity.AppUser;
import br.com.autevia.finkidsapi.domain.enums.UserRole;
import br.com.autevia.finkidsapi.repository.AccountRepository;
import br.com.autevia.finkidsapi.repository.AccountUserRepository;
import br.com.autevia.finkidsapi.repository.AppUserRepository;
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
@ActiveProfiles("test")
@Transactional
class AuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private AccountUserRepository accountUserRepository;

    @Test
    void shouldReturnUnauthorizedWhenJwtIsMissingForUserEndpoint() throws Exception {
        Account account = accountRepository.save(new Account("Luca", "BRL"));

        mockMvc.perform(get("/api/v1/accounts/{accountId}/balance", account.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowReadForChildRole() throws Exception {
        Account account = accountRepository.save(new Account("Mia", "BRL"));
        String childEmail = "child.read@test.com";
        linkAccountUser(account, childEmail, UserRole.CHILD);

        mockMvc.perform(get("/api/v1/accounts/{accountId}/balance", account.getId())
                        .with(jwt().jwt(jwt -> jwt.claim("email", childEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(account.getId()))
                .andExpect(jsonPath("$.currentBalance").value(0.00));
    }

    @Test
    void shouldReturnForbiddenWhenChildAttemptsWriteOperation() throws Exception {
        Account account = accountRepository.save(new Account("Nina", "BRL"));
        String childEmail = "child.write@test.com";
        linkAccountUser(account, childEmail, UserRole.CHILD);

        mockMvc.perform(post("/api/v1/transactions")
                        .with(jwt().jwt(jwt -> jwt.claim("email", childEmail)))
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
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Acesso negado para este recurso."));
    }

    private void linkAccountUser(Account account, String email, UserRole role) {
        AppUser user = appUserRepository.save(
                new AppUser("User " + role.name(), email, "google-" + email, role)
        );
        accountUserRepository.save(new AccountUser(account, user, role));
    }
}

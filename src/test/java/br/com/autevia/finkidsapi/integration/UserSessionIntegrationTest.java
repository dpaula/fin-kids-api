package br.com.autevia.finkidsapi.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserSessionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private AccountUserRepository accountUserRepository;

    @Test
    void shouldReturnCurrentUserContextWithLinkedAccounts() throws Exception {
        String email = "parent.me@test.com";
        AppUser user = appUserRepository.save(new AppUser("Maria", email, "google-parent-me", UserRole.PARENT));
        Account account1 = accountRepository.save(new Account("Luca", "BRL"));
        Account account2 = accountRepository.save(new Account("Nina", "BRL"));

        accountUserRepository.save(new AccountUser(account1, user, UserRole.PARENT));
        accountUserRepository.save(new AccountUser(account2, user, UserRole.CHILD));

        mockMvc.perform(get("/api/v1/users/me")
                        .with(jwt().jwt(jwt -> jwt.claim("email", email))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.globalRole").value("PARENT"))
                .andExpect(jsonPath("$.accounts.length()").value(2))
                .andExpect(jsonPath("$.accounts[0].accountId").value(account1.getId()));
    }

    @Test
    void shouldReturnUnauthorizedWhenJwtIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnNotFoundWhenAuthenticatedUserHasNoAccountLink() throws Exception {
        String email = "orphan.user@test.com";
        appUserRepository.save(new AppUser("Orphan", email, "google-orphan-user", UserRole.PARENT));

        mockMvc.perform(get("/api/v1/users/me")
                        .with(jwt().jwt(jwt -> jwt.claim("email", email))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Usuario sem vinculo de conta para email=" + email));
    }
}

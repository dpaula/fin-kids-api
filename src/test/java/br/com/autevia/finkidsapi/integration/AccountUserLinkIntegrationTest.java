package br.com.autevia.finkidsapi.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.autevia.finkidsapi.domain.entity.Account;
import br.com.autevia.finkidsapi.domain.entity.AccountUser;
import br.com.autevia.finkidsapi.domain.entity.AppUser;
import br.com.autevia.finkidsapi.domain.entity.AuditEvent;
import br.com.autevia.finkidsapi.domain.enums.AuditActionType;
import br.com.autevia.finkidsapi.domain.enums.UserRole;
import br.com.autevia.finkidsapi.repository.AccountRepository;
import br.com.autevia.finkidsapi.repository.AccountUserRepository;
import br.com.autevia.finkidsapi.repository.AppUserRepository;
import br.com.autevia.finkidsapi.repository.AuditEventRepository;
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
@ActiveProfiles("test")
@Transactional
class AccountUserLinkIntegrationTest {

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
    void shouldCreateLinkAndAuditWhenParentHasPermission() throws Exception {
        Account account = accountRepository.save(new Account("Luca", "BRL"));
        AppUser parentActor = appUserRepository.save(
                new AppUser("Parent", "parent.link@test.com", "google-parent-link", UserRole.PARENT)
        );
        AppUser targetUser = appUserRepository.save(
                new AppUser("Child", "child.link@test.com", "google-child-link", UserRole.CHILD)
        );
        accountUserRepository.save(new AccountUser(account, parentActor, UserRole.PARENT));

        mockMvc.perform(put("/api/v1/accounts/{accountId}/user-links/{userId}", account.getId(), targetUser.getId())
                        .with(jwt().jwt(jwt -> jwt.claim("email", parentActor.getEmail())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profileRole": "CHILD"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(account.getId()))
                .andExpect(jsonPath("$.userId").value(targetUser.getId()))
                .andExpect(jsonPath("$.profileRole").value("CHILD"));

        AccountUser link = accountUserRepository.findByAccount_IdAndUser_Id(account.getId(), targetUser.getId()).orElseThrow();
        assertThat(link.getProfileRole()).isEqualTo(UserRole.CHILD);

        List<AuditEvent> events = auditEventRepository.findByAccountIdOrderByCreatedAtAsc(account.getId());
        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getActionType()).isEqualTo(AuditActionType.ACCOUNT_USER_LINK_UPSERTED);
        assertThat(events.getFirst().getActorEmail()).isEqualTo(parentActor.getEmail());
    }

    @Test
    void shouldUpdateRoleForExistingLink() throws Exception {
        Account account = accountRepository.save(new Account("Nina", "BRL"));
        AppUser parentActor = appUserRepository.save(
                new AppUser("Parent", "parent.update@test.com", "google-parent-update", UserRole.PARENT)
        );
        AppUser targetUser = appUserRepository.save(
                new AppUser("User", "user.update@test.com", "google-user-update", UserRole.CHILD)
        );
        accountUserRepository.save(new AccountUser(account, parentActor, UserRole.PARENT));
        accountUserRepository.save(new AccountUser(account, targetUser, UserRole.CHILD));

        mockMvc.perform(put("/api/v1/accounts/{accountId}/user-links/{userId}", account.getId(), targetUser.getId())
                        .with(jwt().jwt(jwt -> jwt.claim("email", parentActor.getEmail())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profileRole": "PARENT"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileRole").value("PARENT"));

        AccountUser updated = accountUserRepository.findByAccount_IdAndUser_Id(account.getId(), targetUser.getId()).orElseThrow();
        assertThat(updated.getProfileRole()).isEqualTo(UserRole.PARENT);
    }

    @Test
    void shouldListLinksForAccount() throws Exception {
        Account account = accountRepository.save(new Account("Theo", "BRL"));
        AppUser parentActor = appUserRepository.save(
                new AppUser("Parent", "parent.list@test.com", "google-parent-list", UserRole.PARENT)
        );
        AppUser targetUser = appUserRepository.save(
                new AppUser("User", "user.list@test.com", "google-user-list", UserRole.CHILD)
        );
        accountUserRepository.save(new AccountUser(account, parentActor, UserRole.PARENT));
        accountUserRepository.save(new AccountUser(account, targetUser, UserRole.CHILD));

        mockMvc.perform(get("/api/v1/accounts/{accountId}/user-links", account.getId())
                        .with(jwt().jwt(jwt -> jwt.claim("email", parentActor.getEmail()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(account.getId()))
                .andExpect(jsonPath("$.links.length()").value(2));
    }

    @Test
    void shouldReturnForbiddenWhenChildAttemptsToManageLinks() throws Exception {
        Account account = accountRepository.save(new Account("Maya", "BRL"));
        AppUser childActor = appUserRepository.save(
                new AppUser("Child", "child.actor@test.com", "google-child-actor", UserRole.CHILD)
        );
        AppUser targetUser = appUserRepository.save(
                new AppUser("Target", "target.user@test.com", "google-target-user", UserRole.CHILD)
        );
        accountUserRepository.save(new AccountUser(account, childActor, UserRole.CHILD));

        mockMvc.perform(put("/api/v1/accounts/{accountId}/user-links/{userId}", account.getId(), targetUser.getId())
                        .with(jwt().jwt(jwt -> jwt.claim("email", childActor.getEmail())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profileRole": "CHILD"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Acesso negado para este recurso."));
    }

    @Test
    void shouldReturnUnauthorizedWhenJwtIsMissing() throws Exception {
        Account account = accountRepository.save(new Account("Lia", "BRL"));
        AppUser targetUser = appUserRepository.save(
                new AppUser("Target", "target.unauth@test.com", "google-target-unauth", UserRole.CHILD)
        );

        mockMvc.perform(put("/api/v1/accounts/{accountId}/user-links/{userId}", account.getId(), targetUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profileRole": "CHILD"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
        Account account = accountRepository.save(new Account("Bia", "BRL"));
        AppUser parentActor = appUserRepository.save(
                new AppUser("Parent", "parent.notfound@test.com", "google-parent-notfound", UserRole.PARENT)
        );
        accountUserRepository.save(new AccountUser(account, parentActor, UserRole.PARENT));

        mockMvc.perform(put("/api/v1/accounts/{accountId}/user-links/{userId}", account.getId(), 999999L)
                        .with(jwt().jwt(jwt -> jwt.claim("email", parentActor.getEmail())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profileRole": "CHILD"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Usuario nao encontrado para id=999999"));
    }
}

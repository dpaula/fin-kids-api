package br.com.autevia.finkidsapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import br.com.autevia.finkidsapi.domain.entity.Account;
import br.com.autevia.finkidsapi.domain.entity.AccountUser;
import br.com.autevia.finkidsapi.domain.entity.AppUser;
import br.com.autevia.finkidsapi.domain.enums.UserRole;
import br.com.autevia.finkidsapi.domain.exception.ResourceNotFoundException;
import br.com.autevia.finkidsapi.domain.exception.UnauthorizedException;
import br.com.autevia.finkidsapi.repository.AccountUserRepository;
import br.com.autevia.finkidsapi.repository.AppUserRepository;
import br.com.autevia.finkidsapi.service.dto.user.UserSessionResult;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserSessionServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    private UserSessionService userSessionService;

    @BeforeEach
    void setUp() {
        userSessionService = new UserSessionService(appUserRepository, accountUserRepository);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnSessionWithLinkedAccounts() {
        String email = "parent.session@test.com";
        setJwtAuthentication(email);

        AppUser user = appUser(10L, "Maria", email, UserRole.PARENT);
        AccountUser link1 = accountUser(1L, account(1L, "Luca"), user, UserRole.PARENT);
        AccountUser link2 = accountUser(2L, account(2L, "Nina"), user, UserRole.CHILD);

        when(appUserRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(accountUserRepository.findByUser_EmailOrderByAccount_IdAsc(email)).thenReturn(List.of(link1, link2));

        UserSessionResult result = userSessionService.getCurrentUserSession();

        assertThat(result.userId()).isEqualTo(10L);
        assertThat(result.fullName()).isEqualTo("Maria");
        assertThat(result.email()).isEqualTo(email);
        assertThat(result.globalRole()).isEqualTo(UserRole.PARENT);
        assertThat(result.accounts()).hasSize(2);
        assertThat(result.accounts().getFirst().accountId()).isEqualTo(1L);
        assertThat(result.accounts().getFirst().profileRole()).isEqualTo(UserRole.PARENT);
    }

    @Test
    void shouldThrowNotFoundWhenUserDoesNotExist() {
        String email = "missing.user@test.com";
        setJwtAuthentication(email);

        when(appUserRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userSessionService.getCurrentUserSession())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Usuario nao encontrado");
    }

    @Test
    void shouldThrowNotFoundWhenUserHasNoLinkedAccounts() {
        String email = "user.without.account@test.com";
        setJwtAuthentication(email);

        AppUser user = appUser(11L, "Rafa", email, UserRole.PARENT);
        when(appUserRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(accountUserRepository.findByUser_EmailOrderByAccount_IdAsc(email)).thenReturn(List.of());

        assertThatThrownBy(() -> userSessionService.getCurrentUserSession())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Usuario sem vinculo de conta");
    }

    @Test
    void shouldThrowUnauthorizedWhenAuthenticationHasNoValidEmail() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("anonymousUser", "n/a", List.of())
        );

        assertThatThrownBy(() -> userSessionService.getCurrentUserSession())
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("claim email ausente");
    }

    private void setJwtAuthentication(String email) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("email", email)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(jwt, "n/a", List.of())
        );
    }

    private AppUser appUser(Long id, String name, String email, UserRole role) {
        AppUser user = new AppUser(name, email, "google-" + email, role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Account account(Long id, String childName) {
        Account account = new Account(childName, "BRL");
        ReflectionTestUtils.setField(account, "id", id);
        return account;
    }

    private AccountUser accountUser(Long id, Account account, AppUser user, UserRole profileRole) {
        AccountUser accountUser = new AccountUser(account, user, profileRole);
        ReflectionTestUtils.setField(accountUser, "id", id);
        return accountUser;
    }
}

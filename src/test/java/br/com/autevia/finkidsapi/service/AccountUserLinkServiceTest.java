package br.com.autevia.finkidsapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.autevia.finkidsapi.domain.entity.Account;
import br.com.autevia.finkidsapi.domain.entity.AccountUser;
import br.com.autevia.finkidsapi.domain.entity.AppUser;
import br.com.autevia.finkidsapi.domain.enums.UserRole;
import br.com.autevia.finkidsapi.domain.exception.ValidationException;
import br.com.autevia.finkidsapi.repository.AccountRepository;
import br.com.autevia.finkidsapi.repository.AccountUserRepository;
import br.com.autevia.finkidsapi.repository.AppUserRepository;
import br.com.autevia.finkidsapi.service.dto.accountuser.AccountUserLinkListResult;
import br.com.autevia.finkidsapi.service.dto.accountuser.AccountUserLinkResult;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AccountUserLinkServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @Mock
    private AuditTrailService auditTrailService;

    private AccountUserLinkService accountUserLinkService;

    @BeforeEach
    void setUp() {
        accountUserLinkService = new AccountUserLinkService(
                accountRepository,
                appUserRepository,
                accountUserRepository,
                auditTrailService
        );
    }

    @Test
    void shouldCreateLinkWhenDoesNotExist() {
        Account account = account(1L);
        AppUser user = user(2L, "Maria", "maria@email.com", UserRole.PARENT);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(user));
        when(accountUserRepository.findByAccount_IdAndUser_Id(1L, 2L)).thenReturn(Optional.empty());
        when(accountUserRepository.save(any(AccountUser.class))).thenAnswer(invocation -> {
            AccountUser link = invocation.getArgument(0);
            ReflectionTestUtils.setField(link, "id", 10L);
            ReflectionTestUtils.setField(link, "createdAt", Instant.parse("2026-03-02T14:00:00Z"));
            return link;
        });

        AccountUserLinkResult result = accountUserLinkService.upsertLink(1L, 2L, UserRole.CHILD);

        assertThat(result.linkId()).isEqualTo(10L);
        assertThat(result.accountId()).isEqualTo(1L);
        assertThat(result.userId()).isEqualTo(2L);
        assertThat(result.profileRole()).isEqualTo(UserRole.CHILD);
        verify(auditTrailService).record(any());
    }

    @Test
    void shouldUpdateRoleWhenLinkAlreadyExists() {
        Account account = account(1L);
        AppUser user = user(2L, "Joao", "joao@email.com", UserRole.PARENT);
        AccountUser existing = accountUser(15L, account, user, UserRole.CHILD);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(appUserRepository.findById(2L)).thenReturn(Optional.of(user));
        when(accountUserRepository.findByAccount_IdAndUser_Id(1L, 2L)).thenReturn(Optional.of(existing));
        when(accountUserRepository.save(existing)).thenReturn(existing);

        AccountUserLinkResult result = accountUserLinkService.upsertLink(1L, 2L, UserRole.PARENT);

        assertThat(result.linkId()).isEqualTo(15L);
        assertThat(result.profileRole()).isEqualTo(UserRole.PARENT);
        verify(auditTrailService).record(any());
    }

    @Test
    void shouldListLinksByAccount() {
        Account account = account(1L);
        AppUser user1 = user(2L, "Maria", "maria@email.com", UserRole.PARENT);
        AppUser user2 = user(3L, "Lucas", "lucas@email.com", UserRole.CHILD);
        AccountUser link1 = accountUser(15L, account, user1, UserRole.PARENT);
        AccountUser link2 = accountUser(16L, account, user2, UserRole.CHILD);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountUserRepository.findByAccount_IdOrderByIdAsc(1L)).thenReturn(List.of(link1, link2));

        AccountUserLinkListResult result = accountUserLinkService.listLinks(1L);

        assertThat(result.accountId()).isEqualTo(1L);
        assertThat(result.links()).hasSize(2);
        assertThat(result.links().getFirst().userEmail()).isEqualTo("maria@email.com");
    }

    @Test
    void shouldRejectWhenProfileRoleIsMissing() {
        assertThatThrownBy(() -> accountUserLinkService.upsertLink(1L, 2L, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("profileRole");
    }

    private Account account(Long id) {
        Account account = new Account("Nina", "BRL");
        ReflectionTestUtils.setField(account, "id", id);
        return account;
    }

    private AppUser user(Long id, String name, String email, UserRole role) {
        AppUser user = new AppUser(name, email, "google-" + email, role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private AccountUser accountUser(Long id, Account account, AppUser user, UserRole profileRole) {
        AccountUser link = new AccountUser(account, user, profileRole);
        ReflectionTestUtils.setField(link, "id", id);
        ReflectionTestUtils.setField(link, "createdAt", Instant.parse("2026-03-02T14:00:00Z"));
        return link;
    }
}

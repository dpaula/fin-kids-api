package br.com.autevia.finkidsapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.autevia.finkidsapi.domain.entity.Account;
import br.com.autevia.finkidsapi.domain.entity.AppUser;
import br.com.autevia.finkidsapi.domain.entity.AuditEvent;
import br.com.autevia.finkidsapi.domain.enums.AuditActionType;
import br.com.autevia.finkidsapi.domain.enums.AuditResourceType;
import br.com.autevia.finkidsapi.domain.enums.UserRole;
import br.com.autevia.finkidsapi.domain.exception.UnauthorizedException;
import br.com.autevia.finkidsapi.repository.AccountRepository;
import br.com.autevia.finkidsapi.repository.AppUserRepository;
import br.com.autevia.finkidsapi.repository.AuditEventRepository;
import br.com.autevia.finkidsapi.service.dto.audit.AuditRecordCommand;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuditTrailServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private AuditEventRepository auditEventRepository;

    private AuditTrailService auditTrailService;

    @BeforeEach
    void setUp() {
        auditTrailService = new AuditTrailService(accountRepository, appUserRepository, auditEventRepository);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldPersistAuditEventWithAuthenticatedActor() {
        Long accountId = 1L;
        String email = "parent.audit@test.com";
        setJwtAuthentication(email);

        Account account = new Account("Luca", "BRL");
        ReflectionTestUtils.setField(account, "id", accountId);
        AppUser user = new AppUser("Parent", email, "google-parent-audit", UserRole.PARENT);
        ReflectionTestUtils.setField(user, "id", 10L);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(appUserRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(auditEventRepository.save(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        auditTrailService.record(new AuditRecordCommand(
                accountId,
                AuditActionType.GOAL_CREATED,
                AuditResourceType.GOAL,
                77L,
                "name=Bicicleta, targetAmount=500.00"
        ));

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());
        AuditEvent saved = captor.getValue();

        assertThat(saved.getAccount().getId()).isEqualTo(accountId);
        assertThat(saved.getActorEmail()).isEqualTo(email);
        assertThat(saved.getActorUser().getId()).isEqualTo(10L);
        assertThat(saved.getActorGlobalRole()).isEqualTo(UserRole.PARENT);
        assertThat(saved.getActionType()).isEqualTo(AuditActionType.GOAL_CREATED);
        assertThat(saved.getResourceType()).isEqualTo(AuditResourceType.GOAL);
        assertThat(saved.getResourceId()).isEqualTo(77L);
    }

    @Test
    void shouldThrowUnauthorizedWhenAuthenticationIsMissing() {
        assertThatThrownBy(() -> auditTrailService.record(new AuditRecordCommand(
                1L,
                AuditActionType.GOAL_CREATED,
                AuditResourceType.GOAL,
                11L,
                "payload"
        )))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Usuario nao autenticado");
    }

    @Test
    void shouldUseAuthenticationNameWhenJwtClaimIsUnavailable() {
        Long accountId = 2L;
        String email = "fallback.name@test.com";
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, "n/a", List.of())
        );

        Account account = new Account("Nina", "BRL");
        ReflectionTestUtils.setField(account, "id", accountId);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(appUserRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(auditEventRepository.save(any(AuditEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        auditTrailService.record(new AuditRecordCommand(
                accountId,
                AuditActionType.BONUS_RULE_UPSERTED,
                AuditResourceType.BONUS_RULE,
                98L,
                "   "
        ));

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());
        AuditEvent saved = captor.getValue();
        assertThat(saved.getActorEmail()).isEqualTo(email);
        assertThat(saved.getActorUser()).isNull();
        assertThat(saved.getActorGlobalRole()).isNull();
        assertThat(saved.getPayloadSummary()).isNull();
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
}

package br.com.autevia.finkidsapi.service;

import br.com.autevia.finkidsapi.domain.entity.Account;
import br.com.autevia.finkidsapi.domain.entity.AppUser;
import br.com.autevia.finkidsapi.domain.entity.AuditEvent;
import br.com.autevia.finkidsapi.domain.enums.UserRole;
import br.com.autevia.finkidsapi.domain.exception.ResourceNotFoundException;
import br.com.autevia.finkidsapi.domain.exception.UnauthorizedException;
import br.com.autevia.finkidsapi.domain.exception.ValidationException;
import br.com.autevia.finkidsapi.repository.AccountRepository;
import br.com.autevia.finkidsapi.repository.AppUserRepository;
import br.com.autevia.finkidsapi.repository.AuditEventRepository;
import br.com.autevia.finkidsapi.service.dto.audit.AuditRecordCommand;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditTrailService {

    private final AccountRepository accountRepository;
    private final AppUserRepository appUserRepository;
    private final AuditEventRepository auditEventRepository;

    public AuditTrailService(
            AccountRepository accountRepository,
            AppUserRepository appUserRepository,
            AuditEventRepository auditEventRepository
    ) {
        this.accountRepository = accountRepository;
        this.appUserRepository = appUserRepository;
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional
    public void record(AuditRecordCommand command) {
        validateCommand(command);
        AuthenticatedActor actor = resolveAuthenticatedActor();

        Account account = accountRepository.findById(command.accountId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Conta nao encontrada para id=" + command.accountId()));

        AuditEvent event = new AuditEvent(
                account,
                actor.user(),
                actor.email(),
                actor.globalRole(),
                command.actionType(),
                command.resourceType(),
                command.resourceId(),
                normalizePayload(command.payloadSummary())
        );

        auditEventRepository.save(event);
    }

    private void validateCommand(AuditRecordCommand command) {
        if (command == null) {
            throw new ValidationException("Comando de auditoria e obrigatorio.");
        }
        if (command.accountId() == null || command.accountId() <= 0) {
            throw new ValidationException("accountId da auditoria deve ser maior que zero.");
        }
        if (command.actionType() == null) {
            throw new ValidationException("actionType da auditoria e obrigatorio.");
        }
        if (command.resourceType() == null) {
            throw new ValidationException("resourceType da auditoria e obrigatorio.");
        }
    }

    private AuthenticatedActor resolveAuthenticatedActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Usuario nao autenticado.");
        }

        String email = resolveEmail(authentication);
        AppUser user = appUserRepository.findByEmail(email).orElse(null);
        UserRole globalRole = user == null ? null : user.getRole();
        return new AuthenticatedActor(email, user, globalRole);
    }

    private String resolveEmail(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            String email = jwt.getClaimAsString("email");
            if (email != null && !email.isBlank()) {
                return email;
            }
        }

        String name = authentication.getName();
        if (name == null || name.isBlank() || "anonymousUser".equals(name)) {
            throw new UnauthorizedException("Token JWT invalido: claim email ausente.");
        }
        return name;
    }

    private String normalizePayload(String payloadSummary) {
        if (payloadSummary == null || payloadSummary.isBlank()) {
            return null;
        }
        return payloadSummary.trim();
    }

    private record AuthenticatedActor(
            String email,
            AppUser user,
            UserRole globalRole
    ) {
    }
}

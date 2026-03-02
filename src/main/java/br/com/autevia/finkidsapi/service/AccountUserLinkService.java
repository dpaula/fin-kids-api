package br.com.autevia.finkidsapi.service;

import br.com.autevia.finkidsapi.domain.entity.Account;
import br.com.autevia.finkidsapi.domain.entity.AccountUser;
import br.com.autevia.finkidsapi.domain.entity.AppUser;
import br.com.autevia.finkidsapi.domain.enums.AuditActionType;
import br.com.autevia.finkidsapi.domain.enums.AuditResourceType;
import br.com.autevia.finkidsapi.domain.enums.UserRole;
import br.com.autevia.finkidsapi.domain.exception.ResourceNotFoundException;
import br.com.autevia.finkidsapi.domain.exception.ValidationException;
import br.com.autevia.finkidsapi.repository.AccountRepository;
import br.com.autevia.finkidsapi.repository.AccountUserRepository;
import br.com.autevia.finkidsapi.repository.AppUserRepository;
import br.com.autevia.finkidsapi.service.dto.accountuser.AccountUserLinkListResult;
import br.com.autevia.finkidsapi.service.dto.accountuser.AccountUserLinkResult;
import br.com.autevia.finkidsapi.service.dto.audit.AuditRecordCommand;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountUserLinkService {

    private final AccountRepository accountRepository;
    private final AppUserRepository appUserRepository;
    private final AccountUserRepository accountUserRepository;
    private final AuditTrailService auditTrailService;

    public AccountUserLinkService(
            AccountRepository accountRepository,
            AppUserRepository appUserRepository,
            AccountUserRepository accountUserRepository,
            AuditTrailService auditTrailService
    ) {
        this.accountRepository = accountRepository;
        this.appUserRepository = appUserRepository;
        this.accountUserRepository = accountUserRepository;
        this.auditTrailService = auditTrailService;
    }

    @Transactional
    public AccountUserLinkResult upsertLink(Long accountId, Long userId, UserRole profileRole) {
        validateIds(accountId, userId);
        validateProfileRole(profileRole);

        Account account = findAccountOrThrow(accountId);
        AppUser user = findUserOrThrow(userId);

        AccountUser existingLink = accountUserRepository.findByAccount_IdAndUser_Id(accountId, userId).orElse(null);
        boolean created = existingLink == null;
        UserRole previousRole = existingLink == null ? null : existingLink.getProfileRole();

        AccountUser link = existingLink == null
                ? new AccountUser(account, user, profileRole)
                : existingLink;

        if (existingLink != null) {
            link.setProfileRole(profileRole);
        }

        AccountUser saved = accountUserRepository.save(link);
        auditTrailService.record(new AuditRecordCommand(
                accountId,
                AuditActionType.ACCOUNT_USER_LINK_UPSERTED,
                AuditResourceType.ACCOUNT_USER_LINK,
                saved.getId(),
                "accountId=%s, userId=%s, created=%s, oldRole=%s, newRole=%s"
                        .formatted(accountId, userId, created, previousRole, profileRole)
        ));

        return toResult(saved);
    }

    @Transactional(readOnly = true)
    public AccountUserLinkListResult listLinks(Long accountId) {
        if (accountId == null || accountId <= 0) {
            throw new ValidationException("accountId deve ser informado e maior que zero.");
        }
        findAccountOrThrow(accountId);

        List<AccountUserLinkResult> links = accountUserRepository.findByAccount_IdOrderByIdAsc(accountId)
                .stream()
                .map(this::toResult)
                .toList();
        return new AccountUserLinkListResult(accountId, links);
    }

    private AccountUserLinkResult toResult(AccountUser link) {
        return new AccountUserLinkResult(
                link.getId(),
                link.getAccount().getId(),
                link.getUser().getId(),
                link.getUser().getFullName(),
                link.getUser().getEmail(),
                link.getProfileRole(),
                link.getCreatedAt()
        );
    }

    private void validateIds(Long accountId, Long userId) {
        if (accountId == null || accountId <= 0) {
            throw new ValidationException("accountId deve ser informado e maior que zero.");
        }
        if (userId == null || userId <= 0) {
            throw new ValidationException("userId deve ser informado e maior que zero.");
        }
    }

    private void validateProfileRole(UserRole profileRole) {
        if (profileRole == null) {
            throw new ValidationException("profileRole e obrigatorio.");
        }
    }

    private Account findAccountOrThrow(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta nao encontrada para id=" + accountId));
    }

    private AppUser findUserOrThrow(Long userId) {
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado para id=" + userId));
    }
}

package br.com.autevia.finkidsapi.security;

import br.com.autevia.finkidsapi.domain.enums.UserRole;
import br.com.autevia.finkidsapi.repository.AccountUserRepository;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service("accountAuthorization")
public class AccountAuthorizationService {

    private static final Set<UserRole> READ_ROLES = EnumSet.of(UserRole.CHILD, UserRole.PARENT);
    private static final Set<UserRole> WRITE_ROLES = EnumSet.of(UserRole.PARENT);

    private final AccountUserRepository accountUserRepository;

    public AccountAuthorizationService(AccountUserRepository accountUserRepository) {
        this.accountUserRepository = accountUserRepository;
    }

    public boolean canRead(Long accountId) {
        return hasRoleForAccount(accountId, READ_ROLES);
    }

    public boolean canWrite(Long accountId) {
        return hasRoleForAccount(accountId, WRITE_ROLES);
    }

    private boolean hasRoleForAccount(Long accountId, Set<UserRole> allowedRoles) {
        if (accountId == null || accountId <= 0) {
            return false;
        }

        String email = resolveAuthenticatedEmail();
        if (email == null || email.isBlank()) {
            return false;
        }

        return accountUserRepository.existsByAccount_IdAndUser_EmailAndProfileRoleIn(accountId, email, allowedRoles);
    }

    private String resolveAuthenticatedEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            return jwt.getClaimAsString("email");
        }

        String name = authentication.getName();
        if (name == null || name.isBlank() || "anonymousUser".equals(name)) {
            return null;
        }

        return name;
    }
}

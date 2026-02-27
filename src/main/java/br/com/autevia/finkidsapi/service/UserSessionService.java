package br.com.autevia.finkidsapi.service;

import br.com.autevia.finkidsapi.domain.entity.AccountUser;
import br.com.autevia.finkidsapi.domain.entity.AppUser;
import br.com.autevia.finkidsapi.domain.exception.ResourceNotFoundException;
import br.com.autevia.finkidsapi.domain.exception.UnauthorizedException;
import br.com.autevia.finkidsapi.repository.AccountUserRepository;
import br.com.autevia.finkidsapi.repository.AppUserRepository;
import br.com.autevia.finkidsapi.service.dto.user.UserAccountContextResult;
import br.com.autevia.finkidsapi.service.dto.user.UserSessionResult;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserSessionService {

    private final AppUserRepository appUserRepository;
    private final AccountUserRepository accountUserRepository;

    public UserSessionService(AppUserRepository appUserRepository, AccountUserRepository accountUserRepository) {
        this.appUserRepository = appUserRepository;
        this.accountUserRepository = accountUserRepository;
    }

    @Transactional(readOnly = true)
    public UserSessionResult getCurrentUserSession() {
        String email = resolveAuthenticatedEmail();
        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado para email=" + email));

        List<AccountUser> accountLinks = accountUserRepository.findByUser_EmailOrderByAccount_IdAsc(email);
        if (accountLinks.isEmpty()) {
            throw new ResourceNotFoundException("Usuario sem vinculo de conta para email=" + email);
        }

        List<UserAccountContextResult> accounts = accountLinks.stream()
                .map(link -> new UserAccountContextResult(
                        link.getAccount().getId(),
                        link.getAccount().getChildName(),
                        link.getProfileRole()
                ))
                .toList();

        return new UserSessionResult(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                accounts
        );
    }

    private String resolveAuthenticatedEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Usuario nao autenticado.");
        }

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
}

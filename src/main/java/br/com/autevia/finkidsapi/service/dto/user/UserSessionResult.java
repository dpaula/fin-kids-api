package br.com.autevia.finkidsapi.service.dto.user;

import br.com.autevia.finkidsapi.domain.enums.UserRole;
import java.util.List;

public record UserSessionResult(
        Long userId,
        String fullName,
        String email,
        UserRole globalRole,
        List<UserAccountContextResult> accounts
) {
}

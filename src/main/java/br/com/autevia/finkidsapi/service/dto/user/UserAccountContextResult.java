package br.com.autevia.finkidsapi.service.dto.user;

import br.com.autevia.finkidsapi.domain.enums.UserRole;

public record UserAccountContextResult(
        Long accountId,
        String childName,
        UserRole profileRole
) {
}

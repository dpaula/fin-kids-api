package br.com.autevia.finkidsapi.service.dto.accountuser;

import br.com.autevia.finkidsapi.domain.enums.UserRole;
import java.time.Instant;

public record AccountUserLinkResult(
        Long linkId,
        Long accountId,
        Long userId,
        String userFullName,
        String userEmail,
        UserRole profileRole,
        Instant linkedAt
) {
}

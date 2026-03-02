package br.com.autevia.finkidsapi.service.dto.accountuser;

import java.util.List;

public record AccountUserLinkListResult(
        Long accountId,
        List<AccountUserLinkResult> links
) {
}

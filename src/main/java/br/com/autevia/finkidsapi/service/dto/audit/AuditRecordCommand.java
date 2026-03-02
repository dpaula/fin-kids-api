package br.com.autevia.finkidsapi.service.dto.audit;

import br.com.autevia.finkidsapi.domain.enums.AuditActionType;
import br.com.autevia.finkidsapi.domain.enums.AuditResourceType;

public record AuditRecordCommand(
        Long accountId,
        AuditActionType actionType,
        AuditResourceType resourceType,
        Long resourceId,
        String payloadSummary
) {
}

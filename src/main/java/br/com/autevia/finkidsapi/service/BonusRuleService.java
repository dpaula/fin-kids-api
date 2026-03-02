package br.com.autevia.finkidsapi.service;

import br.com.autevia.finkidsapi.domain.entity.Account;
import br.com.autevia.finkidsapi.domain.entity.BonusRule;
import br.com.autevia.finkidsapi.domain.enums.AuditActionType;
import br.com.autevia.finkidsapi.domain.enums.AuditResourceType;
import br.com.autevia.finkidsapi.domain.exception.ResourceNotFoundException;
import br.com.autevia.finkidsapi.domain.exception.ValidationException;
import br.com.autevia.finkidsapi.repository.AccountRepository;
import br.com.autevia.finkidsapi.repository.BonusRuleRepository;
import br.com.autevia.finkidsapi.service.dto.audit.AuditRecordCommand;
import br.com.autevia.finkidsapi.service.dto.bonus.BonusRuleResult;
import br.com.autevia.finkidsapi.service.dto.bonus.UpdateBonusRuleCommand;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BonusRuleService {

    private static final BigDecimal MIN_PERCENTAGE = new BigDecimal("0.01");
    private static final BigDecimal MAX_PERCENTAGE = new BigDecimal("100.00");

    private final AccountRepository accountRepository;
    private final BonusRuleRepository bonusRuleRepository;
    private final AuditTrailService auditTrailService;

    public BonusRuleService(
            AccountRepository accountRepository,
            BonusRuleRepository bonusRuleRepository,
            AuditTrailService auditTrailService
    ) {
        this.accountRepository = accountRepository;
        this.bonusRuleRepository = bonusRuleRepository;
        this.auditTrailService = auditTrailService;
    }

    @Transactional(readOnly = true)
    public BonusRuleResult getRule(Long accountId) {
        validateAccountId(accountId);
        findAccountOrThrow(accountId);

        BonusRule rule = bonusRuleRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Regra de bonus nao encontrada para accountId=" + accountId));

        return toResult(rule);
    }

    @Transactional
    public BonusRuleResult upsertRule(Long accountId, UpdateBonusRuleCommand command) {
        validateAccountId(accountId);
        validateCommand(command);

        Account account = findAccountOrThrow(accountId);
        Optional<BonusRule> existingRule = bonusRuleRepository.findByAccountId(accountId);
        BonusRule rule = existingRule
                .map(existing -> updateExistingRule(existing, command))
                .orElseGet(() -> new BonusRule(
                        account,
                        command.percentage(),
                        command.conditionType(),
                        command.baseType(),
                        command.active()
                ));

        BonusRule saved = bonusRuleRepository.save(rule);
        boolean created = existingRule.isEmpty();
        auditTrailService.record(new AuditRecordCommand(
                accountId,
                AuditActionType.BONUS_RULE_UPSERTED,
                AuditResourceType.BONUS_RULE,
                saved.getId(),
                buildAuditPayload(saved, created)
        ));
        return toResult(saved);
    }

    private BonusRule updateExistingRule(BonusRule existing, UpdateBonusRuleCommand command) {
        existing.setPercentage(command.percentage());
        existing.setConditionType(command.conditionType());
        existing.setBaseType(command.baseType());
        existing.setActive(command.active());
        return existing;
    }

    private void validateCommand(UpdateBonusRuleCommand command) {
        if (command == null) {
            throw new ValidationException("Dados da regra de bonus sao obrigatorios.");
        }

        if (command.percentage() == null
                || command.percentage().compareTo(MIN_PERCENTAGE) < 0
                || command.percentage().compareTo(MAX_PERCENTAGE) > 0) {
            throw new ValidationException("percentage deve estar entre 0.01 e 100.00.");
        }

        if (command.conditionType() == null) {
            throw new ValidationException("conditionType e obrigatorio.");
        }

        if (command.baseType() == null) {
            throw new ValidationException("baseType e obrigatorio.");
        }

        if (command.active() == null) {
            throw new ValidationException("active e obrigatorio.");
        }
    }

    private void validateAccountId(Long accountId) {
        if (accountId == null || accountId <= 0) {
            throw new ValidationException("accountId deve ser informado e maior que zero.");
        }
    }

    private Account findAccountOrThrow(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta nao encontrada para id=" + accountId));
    }

    private BonusRuleResult toResult(BonusRule rule) {
        return new BonusRuleResult(
                rule.getId(),
                rule.getAccount().getId(),
                rule.getPercentage(),
                rule.getConditionType(),
                rule.getBaseType(),
                rule.isActive(),
                rule.getCreatedAt(),
                rule.getUpdatedAt()
        );
    }

    private String buildAuditPayload(BonusRule saved, boolean created) {
        return "created=%s, percentage=%s, conditionType=%s, baseType=%s, active=%s"
                .formatted(
                        created,
                        saved.getPercentage(),
                        saved.getConditionType(),
                        saved.getBaseType(),
                        saved.isActive()
                );
    }
}

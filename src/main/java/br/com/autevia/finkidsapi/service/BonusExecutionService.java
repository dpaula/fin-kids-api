package br.com.autevia.finkidsapi.service;

import br.com.autevia.finkidsapi.domain.entity.AccountTransaction;
import br.com.autevia.finkidsapi.domain.entity.BonusRule;
import br.com.autevia.finkidsapi.domain.enums.AuditActionType;
import br.com.autevia.finkidsapi.domain.enums.AuditResourceType;
import br.com.autevia.finkidsapi.domain.enums.BonusBaseType;
import br.com.autevia.finkidsapi.domain.enums.TransactionOrigin;
import br.com.autevia.finkidsapi.domain.enums.TransactionType;
import br.com.autevia.finkidsapi.repository.AccountTransactionRepository;
import br.com.autevia.finkidsapi.repository.BonusRuleRepository;
import br.com.autevia.finkidsapi.service.dto.CreateTransactionCommand;
import br.com.autevia.finkidsapi.service.dto.CreateTransactionResult;
import br.com.autevia.finkidsapi.service.dto.audit.AuditRecordCommand;
import br.com.autevia.finkidsapi.service.dto.bonus.BonusExecutionSummary;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BonusExecutionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BonusExecutionService.class);
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final ZoneOffset BONUS_ZONE = ZoneOffset.UTC;

    private final BonusRuleRepository bonusRuleRepository;
    private final AccountTransactionRepository accountTransactionRepository;
    private final TransactionService transactionService;
    private final AuditTrailService auditTrailService;
    private final String systemActorEmail;

    public BonusExecutionService(
            BonusRuleRepository bonusRuleRepository,
            AccountTransactionRepository accountTransactionRepository,
            TransactionService transactionService,
            AuditTrailService auditTrailService,
            @Value("${app.bonus.execution.actor-email:system.bonus@granagalaxy.local}") String systemActorEmail
    ) {
        this.bonusRuleRepository = bonusRuleRepository;
        this.accountTransactionRepository = accountTransactionRepository;
        this.transactionService = transactionService;
        this.auditTrailService = auditTrailService;
        this.systemActorEmail = systemActorEmail;
    }

    public BonusExecutionSummary executeForReferenceMonth(YearMonth referenceMonth) {
        if (referenceMonth == null) {
            throw new IllegalArgumentException("referenceMonth e obrigatorio.");
        }

        Instant periodStart = referenceMonth.atDay(1).atStartOfDay().toInstant(BONUS_ZONE);
        Instant periodEnd = referenceMonth.plusMonths(1).atDay(1).atStartOfDay().toInstant(BONUS_ZONE);
        String evidenceReference = buildEvidenceReference(referenceMonth);

        List<BonusRule> activeRules = bonusRuleRepository.findByActiveTrue();
        int eligibleRules = 0;
        int appliedBonuses = 0;
        int skippedRules = 0;
        int failedRules = 0;

        for (BonusRule rule : activeRules) {
            try {
                ProcessOutcome outcome = processRule(rule, periodStart, periodEnd, referenceMonth, evidenceReference);
                if (outcome == ProcessOutcome.ELIGIBLE_APPLIED) {
                    eligibleRules++;
                    appliedBonuses++;
                } else if (outcome == ProcessOutcome.ELIGIBLE_SKIPPED) {
                    eligibleRules++;
                    skippedRules++;
                } else {
                    skippedRules++;
                }
            } catch (RuntimeException ex) {
                failedRules++;
                LOGGER.warn(
                        "Failed to execute monthly bonus for accountId={} ruleId={} month={}: {}",
                        rule.getAccount().getId(),
                        rule.getId(),
                        referenceMonth,
                        ex.getMessage()
                );
            }
        }

        return new BonusExecutionSummary(
                activeRules.size(),
                eligibleRules,
                appliedBonuses,
                skippedRules,
                failedRules
        );
    }

    private ProcessOutcome processRule(
            BonusRule rule,
            Instant periodStart,
            Instant periodEnd,
            YearMonth referenceMonth,
            String evidenceReference
    ) {
        Long accountId = rule.getAccount().getId();
        if (accountId == null) {
            return ProcessOutcome.NOT_ELIGIBLE;
        }

        boolean alreadyApplied = accountTransactionRepository.existsByAccount_IdAndOriginAndEvidenceReference(
                accountId,
                TransactionOrigin.BONUS,
                evidenceReference
        );
        if (alreadyApplied) {
            return ProcessOutcome.NOT_ELIGIBLE;
        }

        boolean hasWithdrawals = accountTransactionRepository.existsByAccountIdAndTypeAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
                accountId,
                TransactionType.WITHDRAW,
                periodStart,
                periodEnd
        );
        if (hasWithdrawals) {
            return ProcessOutcome.NOT_ELIGIBLE;
        }

        BigDecimal baseAmount = resolveBaseAmount(rule, periodStart, periodEnd);
        if (baseAmount.compareTo(ZERO) <= 0) {
            return ProcessOutcome.ELIGIBLE_SKIPPED;
        }

        BigDecimal bonusAmount = baseAmount
                .multiply(rule.getPercentage())
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);

        if (bonusAmount.compareTo(ZERO) <= 0) {
            return ProcessOutcome.ELIGIBLE_SKIPPED;
        }

        CreateTransactionResult created = transactionService.createTransaction(new CreateTransactionCommand(
                accountId,
                TransactionType.DEPOSIT,
                TransactionOrigin.BONUS,
                bonusAmount,
                buildBonusDescription(referenceMonth, rule.getPercentage()),
                evidenceReference,
                periodEnd
        ));

        auditTrailService.recordSystem(new AuditRecordCommand(
                accountId,
                AuditActionType.BONUS_APPLIED,
                AuditResourceType.TRANSACTION,
                created.transactionId(),
                buildAuditPayload(referenceMonth, rule.getPercentage(), baseAmount, bonusAmount, evidenceReference)
        ), systemActorEmail);

        return ProcessOutcome.ELIGIBLE_APPLIED;
    }

    private BigDecimal resolveBaseAmount(BonusRule rule, Instant periodStart, Instant periodEnd) {
        Long accountId = rule.getAccount().getId();
        BonusBaseType baseType = rule.getBaseType();

        return switch (baseType) {
            case LAST_BALANCE -> defaultZero(accountTransactionRepository.calculateBalanceByAccountIdBefore(accountId, periodEnd));
            case LAST_ALLOWANCE -> {
                AccountTransaction lastAllowance = accountTransactionRepository
                        .findTopByAccountIdAndTypeAndOriginNotAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtDesc(
                                accountId,
                                TransactionType.DEPOSIT,
                                TransactionOrigin.BONUS,
                                periodStart,
                                periodEnd
                        );
                yield lastAllowance == null ? ZERO : defaultZero(lastAllowance.getAmount());
            }
            case MONTHLY_DEPOSITS -> defaultZero(accountTransactionRepository
                    .sumAmountByAccountIdAndTypeAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
                            accountId,
                            TransactionType.DEPOSIT,
                            periodStart,
                            periodEnd
                    ));
        };
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private String buildEvidenceReference(YearMonth referenceMonth) {
        return "bonus:%s".formatted(referenceMonth);
    }

    private String buildBonusDescription(YearMonth referenceMonth, BigDecimal percentage) {
        return "Bonus por guardar %s%% (%s)".formatted(percentage, referenceMonth);
    }

    private String buildAuditPayload(
            YearMonth referenceMonth,
            BigDecimal percentage,
            BigDecimal baseAmount,
            BigDecimal bonusAmount,
            String evidenceReference
    ) {
        return "referenceMonth=%s, percentage=%s, baseAmount=%s, bonusAmount=%s, evidenceReference=%s"
                .formatted(referenceMonth, percentage, baseAmount, bonusAmount, evidenceReference);
    }

    private enum ProcessOutcome {
        NOT_ELIGIBLE,
        ELIGIBLE_SKIPPED,
        ELIGIBLE_APPLIED
    }
}

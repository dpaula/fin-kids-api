package br.com.autevia.finkidsapi.service;

import br.com.autevia.finkidsapi.service.dto.bonus.BonusExecutionSummary;
import java.time.YearMonth;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.bonus.execution", name = "enabled", havingValue = "true")
public class BonusExecutionScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(BonusExecutionScheduler.class);

    private final BonusExecutionService bonusExecutionService;
    private final ZoneId executionZone;

    public BonusExecutionScheduler(
            BonusExecutionService bonusExecutionService,
            @Value("${app.bonus.execution.zone:UTC}") String executionZone
    ) {
        this.bonusExecutionService = bonusExecutionService;
        this.executionZone = ZoneId.of(executionZone);
    }

    @Scheduled(
            cron = "${app.bonus.execution.cron:0 5 0 1 * *}",
            zone = "${app.bonus.execution.zone:UTC}"
    )
    public void executeMonthlyBonus() {
        YearMonth referenceMonth = YearMonth.now(executionZone).minusMonths(1);
        BonusExecutionSummary summary = bonusExecutionService.executeForReferenceMonth(referenceMonth);
        LOGGER.info(
                "Bonus execution finished for referenceMonth={} totalRules={} eligible={} applied={} skipped={} failed={}",
                referenceMonth,
                summary.totalRules(),
                summary.eligibleRules(),
                summary.appliedBonuses(),
                summary.skippedRules(),
                summary.failedRules()
        );
    }
}

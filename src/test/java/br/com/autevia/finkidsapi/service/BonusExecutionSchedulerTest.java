package br.com.autevia.finkidsapi.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.autevia.finkidsapi.service.dto.bonus.BonusExecutionSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BonusExecutionSchedulerTest {

    @Mock
    private BonusExecutionService bonusExecutionService;

    private BonusExecutionScheduler bonusExecutionScheduler;

    @BeforeEach
    void setUp() {
        bonusExecutionScheduler = new BonusExecutionScheduler(bonusExecutionService, "UTC");
    }

    @Test
    void shouldDelegateExecutionToBonusService() {
        when(bonusExecutionService.executeForReferenceMonth(any()))
                .thenReturn(new BonusExecutionSummary(0, 0, 0, 0, 0));

        bonusExecutionScheduler.executeMonthlyBonus();

        verify(bonusExecutionService).executeForReferenceMonth(any());
    }
}

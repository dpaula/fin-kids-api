package br.com.autevia.finkidsapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.autevia.finkidsapi.domain.entity.Account;
import br.com.autevia.finkidsapi.domain.entity.Goal;
import br.com.autevia.finkidsapi.domain.exception.ValidationException;
import br.com.autevia.finkidsapi.repository.AccountRepository;
import br.com.autevia.finkidsapi.repository.GoalRepository;
import br.com.autevia.finkidsapi.service.dto.goal.CreateGoalCommand;
import br.com.autevia.finkidsapi.service.dto.goal.GoalItemResult;
import br.com.autevia.finkidsapi.service.dto.goal.GoalListResult;
import br.com.autevia.finkidsapi.service.dto.goal.UpdateGoalCommand;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private GoalRepository goalRepository;

    private GoalService goalService;

    @BeforeEach
    void setUp() {
        goalService = new GoalService(accountRepository, goalRepository);
    }

    @Test
    void shouldCreateGoal() {
        Account account = account(1L);

        CreateGoalCommand command = new CreateGoalCommand(1L, "Bicicleta", new BigDecimal("500.00"));

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(goalRepository.save(any(Goal.class))).thenAnswer(invocation -> {
            Goal goal = invocation.getArgument(0);
            ReflectionTestUtils.setField(goal, "id", 10L);
            ReflectionTestUtils.setField(goal, "createdAt", Instant.parse("2026-02-27T12:00:00Z"));
            ReflectionTestUtils.setField(goal, "updatedAt", Instant.parse("2026-02-27T12:00:00Z"));
            return goal;
        });

        GoalItemResult result = goalService.createGoal(command);

        assertThat(result.goalId()).isEqualTo(10L);
        assertThat(result.accountId()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Bicicleta");
        assertThat(result.targetAmount()).isEqualByComparingTo("500.00");
        assertThat(result.active()).isTrue();
    }

    @Test
    void shouldListActiveGoalsByAccount() {
        Account account = account(1L);

        Goal goal1 = goal(11L, account, "Bicicleta", "500.00", true);
        Goal goal2 = goal(12L, account, "Video Game", "1500.00", true);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(goalRepository.findByAccountIdAndActiveTrue(1L)).thenReturn(List.of(goal1, goal2));

        GoalListResult result = goalService.listGoals(1L);

        assertThat(result.accountId()).isEqualTo(1L);
        assertThat(result.goals()).hasSize(2);
        assertThat(result.goals().getFirst().name()).isEqualTo("Bicicleta");
    }

    @Test
    void shouldUpdateGoal() {
        Account account = account(1L);
        Goal existing = goal(11L, account, "Bicicleta", "500.00", true);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(goalRepository.findByIdAndAccountIdAndActiveTrue(11L, 1L)).thenReturn(Optional.of(existing));
        when(goalRepository.save(existing)).thenReturn(existing);

        GoalItemResult result = goalService.updateGoal(
                11L,
                new UpdateGoalCommand(1L, "Notebook", new BigDecimal("3200.00"))
        );

        assertThat(result.goalId()).isEqualTo(11L);
        assertThat(result.name()).isEqualTo("Notebook");
        assertThat(result.targetAmount()).isEqualByComparingTo("3200.00");
    }

    @Test
    void shouldSoftDeleteGoal() {
        Account account = account(1L);
        Goal existing = goal(11L, account, "Bicicleta", "500.00", true);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(goalRepository.findByIdAndAccountIdAndActiveTrue(11L, 1L)).thenReturn(Optional.of(existing));

        goalService.deleteGoal(11L, 1L);

        assertThat(existing.isActive()).isFalse();
        verify(goalRepository).save(existing);
    }

    @Test
    void shouldRejectInvalidTargetAmount() {
        CreateGoalCommand command = new CreateGoalCommand(1L, "Bicicleta", BigDecimal.ZERO);

        assertThatThrownBy(() -> goalService.createGoal(command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("targetAmount");
    }

    private Account account(Long id) {
        Account account = new Account("Lucas", "BRL");
        ReflectionTestUtils.setField(account, "id", id);
        return account;
    }

    private Goal goal(Long id, Account account, String name, String amount, boolean active) {
        Goal goal = new Goal(account, name, new BigDecimal(amount), active);
        ReflectionTestUtils.setField(goal, "id", id);
        ReflectionTestUtils.setField(goal, "createdAt", Instant.parse("2026-02-27T12:00:00Z"));
        ReflectionTestUtils.setField(goal, "updatedAt", Instant.parse("2026-02-27T12:00:00Z"));
        return goal;
    }
}

package br.com.autevia.finkidsapi.service;

import br.com.autevia.finkidsapi.domain.entity.Account;
import br.com.autevia.finkidsapi.domain.entity.Goal;
import br.com.autevia.finkidsapi.domain.enums.AuditActionType;
import br.com.autevia.finkidsapi.domain.enums.AuditResourceType;
import br.com.autevia.finkidsapi.domain.exception.ResourceNotFoundException;
import br.com.autevia.finkidsapi.domain.exception.ValidationException;
import br.com.autevia.finkidsapi.repository.AccountRepository;
import br.com.autevia.finkidsapi.repository.GoalRepository;
import br.com.autevia.finkidsapi.service.dto.audit.AuditRecordCommand;
import br.com.autevia.finkidsapi.service.dto.goal.CreateGoalCommand;
import br.com.autevia.finkidsapi.service.dto.goal.GoalItemResult;
import br.com.autevia.finkidsapi.service.dto.goal.GoalListResult;
import br.com.autevia.finkidsapi.service.dto.goal.UpdateGoalCommand;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoalService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final AccountRepository accountRepository;
    private final GoalRepository goalRepository;
    private final AuditTrailService auditTrailService;

    public GoalService(
            AccountRepository accountRepository,
            GoalRepository goalRepository,
            AuditTrailService auditTrailService
    ) {
        this.accountRepository = accountRepository;
        this.goalRepository = goalRepository;
        this.auditTrailService = auditTrailService;
    }

    @Transactional
    public GoalItemResult createGoal(CreateGoalCommand command) {
        validateCreateOrUpdateCommand(command.accountId(), command.name(), command.targetAmount());

        Account account = findAccountOrThrow(command.accountId());
        Goal goal = new Goal(account, command.name().trim(), command.targetAmount(), true);

        Goal saved = goalRepository.save(goal);
        auditTrailService.record(new AuditRecordCommand(
                account.getId(),
                AuditActionType.GOAL_CREATED,
                AuditResourceType.GOAL,
                saved.getId(),
                "name=%s, targetAmount=%s, active=%s"
                        .formatted(saved.getName(), saved.getTargetAmount(), saved.isActive())
        ));
        return toResult(saved);
    }

    @Transactional(readOnly = true)
    public GoalListResult listGoals(Long accountId) {
        validateAccountId(accountId);
        findAccountOrThrow(accountId);

        List<GoalItemResult> items = goalRepository.findByAccountIdAndActiveTrue(accountId).stream()
                .map(this::toResult)
                .toList();

        return new GoalListResult(accountId, items);
    }

    @Transactional
    public GoalItemResult updateGoal(Long goalId, UpdateGoalCommand command) {
        validateGoalId(goalId);
        validateCreateOrUpdateCommand(command.accountId(), command.name(), command.targetAmount());
        findAccountOrThrow(command.accountId());

        Goal goal = goalRepository.findByIdAndAccountIdAndActiveTrue(goalId, command.accountId())
                .orElseThrow(() -> new ResourceNotFoundException("Meta nao encontrada para id=" + goalId));

        String previousName = goal.getName();
        BigDecimal previousTargetAmount = goal.getTargetAmount();
        goal.setName(command.name().trim());
        goal.setTargetAmount(command.targetAmount());

        Goal saved = goalRepository.save(goal);
        auditTrailService.record(new AuditRecordCommand(
                command.accountId(),
                AuditActionType.GOAL_UPDATED,
                AuditResourceType.GOAL,
                saved.getId(),
                "oldName=%s, newName=%s, oldTargetAmount=%s, newTargetAmount=%s"
                        .formatted(previousName, saved.getName(), previousTargetAmount, saved.getTargetAmount())
        ));
        return toResult(saved);
    }

    @Transactional
    public void deleteGoal(Long goalId, Long accountId) {
        validateGoalId(goalId);
        validateAccountId(accountId);
        findAccountOrThrow(accountId);

        Goal goal = goalRepository.findByIdAndAccountIdAndActiveTrue(goalId, accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Meta nao encontrada para id=" + goalId));

        String goalName = goal.getName();
        BigDecimal goalTargetAmount = goal.getTargetAmount();
        goal.setActive(false);
        goalRepository.save(goal);
        auditTrailService.record(new AuditRecordCommand(
                accountId,
                AuditActionType.GOAL_DELETED,
                AuditResourceType.GOAL,
                goalId,
                "name=%s, targetAmount=%s, active=false".formatted(goalName, goalTargetAmount)
        ));
    }

    private void validateCreateOrUpdateCommand(Long accountId, String name, BigDecimal targetAmount) {
        validateAccountId(accountId);

        if (name == null || name.isBlank()) {
            throw new ValidationException("name e obrigatorio.");
        }

        if (targetAmount == null || targetAmount.compareTo(ZERO) <= 0) {
            throw new ValidationException("targetAmount deve ser maior que zero.");
        }
    }

    private void validateAccountId(Long accountId) {
        if (accountId == null || accountId <= 0) {
            throw new ValidationException("accountId deve ser informado e maior que zero.");
        }
    }

    private void validateGoalId(Long goalId) {
        if (goalId == null || goalId <= 0) {
            throw new ValidationException("goalId deve ser informado e maior que zero.");
        }
    }

    private Account findAccountOrThrow(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta nao encontrada para id=" + accountId));
    }

    private GoalItemResult toResult(Goal goal) {
        return new GoalItemResult(
                goal.getId(),
                goal.getAccount().getId(),
                goal.getName(),
                goal.getTargetAmount(),
                goal.isActive(),
                goal.getCreatedAt(),
                goal.getUpdatedAt()
        );
    }
}

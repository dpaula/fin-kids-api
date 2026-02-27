package br.com.autevia.finkidsapi.web;

import br.com.autevia.finkidsapi.service.GoalService;
import br.com.autevia.finkidsapi.service.dto.goal.CreateGoalCommand;
import br.com.autevia.finkidsapi.service.dto.goal.GoalItemResult;
import br.com.autevia.finkidsapi.service.dto.goal.GoalListResult;
import br.com.autevia.finkidsapi.service.dto.goal.UpdateGoalCommand;
import br.com.autevia.finkidsapi.web.dto.goal.CreateGoalRequest;
import br.com.autevia.finkidsapi.web.dto.goal.GoalListResponse;
import br.com.autevia.finkidsapi.web.dto.goal.GoalResponse;
import br.com.autevia.finkidsapi.web.dto.goal.UpdateGoalRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/goals")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GoalResponse create(@RequestBody CreateGoalRequest request) {
        GoalItemResult result = goalService.createGoal(
                new CreateGoalCommand(request.accountId(), request.name(), request.targetAmount())
        );
        return toResponse(result);
    }

    @GetMapping
    public GoalListResponse list(@RequestParam Long accountId) {
        GoalListResult result = goalService.listGoals(accountId);
        List<GoalResponse> goals = result.goals().stream()
                .map(this::toResponse)
                .toList();

        return new GoalListResponse(result.accountId(), goals);
    }

    @PutMapping("/{goalId}")
    public GoalResponse update(@PathVariable Long goalId, @RequestBody UpdateGoalRequest request) {
        GoalItemResult result = goalService.updateGoal(
                goalId,
                new UpdateGoalCommand(request.accountId(), request.name(), request.targetAmount())
        );
        return toResponse(result);
    }

    @DeleteMapping("/{goalId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long goalId, @RequestParam Long accountId) {
        goalService.deleteGoal(goalId, accountId);
    }

    private GoalResponse toResponse(GoalItemResult item) {
        return new GoalResponse(
                item.goalId(),
                item.accountId(),
                item.name(),
                item.targetAmount(),
                item.active(),
                item.createdAt(),
                item.updatedAt()
        );
    }
}

package br.com.autevia.finkidsapi.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.autevia.finkidsapi.domain.exception.ResourceNotFoundException;
import br.com.autevia.finkidsapi.service.GoalService;
import br.com.autevia.finkidsapi.service.dto.goal.GoalItemResult;
import br.com.autevia.finkidsapi.service.dto.goal.GoalListResult;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GoalController.class)
@Import(ApiExceptionHandler.class)
class GoalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GoalService goalService;

    @Test
    void shouldCreateGoal() throws Exception {
        GoalItemResult result = goalResult(11L, 1L, "Bicicleta", "500.00", true);
        when(goalService.createGoal(any())).thenReturn(result);

        String payload = """
                {
                  "accountId": 1,
                  "name": "Bicicleta",
                  "targetAmount": 500.00
                }
                """;

        mockMvc.perform(post("/api/v1/goals")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.goalId").value(11))
                .andExpect(jsonPath("$.name").value("Bicicleta"))
                .andExpect(jsonPath("$.targetAmount").value(500.00));
    }

    @Test
    void shouldListGoals() throws Exception {
        GoalItemResult goal1 = goalResult(11L, 1L, "Bicicleta", "500.00", true);
        GoalItemResult goal2 = goalResult(12L, 1L, "Video Game", "1500.00", true);

        when(goalService.listGoals(1L)).thenReturn(new GoalListResult(1L, List.of(goal1, goal2)));

        mockMvc.perform(get("/api/v1/goals").param("accountId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(1))
                .andExpect(jsonPath("$.goals[0].goalId").value(11))
                .andExpect(jsonPath("$.goals[1].name").value("Video Game"));
    }

    @Test
    void shouldUpdateGoal() throws Exception {
        GoalItemResult result = goalResult(11L, 1L, "Notebook", "3200.00", true);
        when(goalService.updateGoal(org.mockito.ArgumentMatchers.eq(11L), any())).thenReturn(result);

        String payload = """
                {
                  "accountId": 1,
                  "name": "Notebook",
                  "targetAmount": 3200.00
                }
                """;

        mockMvc.perform(put("/api/v1/goals/11")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.goalId").value(11))
                .andExpect(jsonPath("$.name").value("Notebook"));
    }

    @Test
    void shouldDeleteGoal() throws Exception {
        doNothing().when(goalService).deleteGoal(11L, 1L);

        mockMvc.perform(delete("/api/v1/goals/11").param("accountId", "1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturnNotFoundWhenGoalDoesNotExist() throws Exception {
        when(goalService.updateGoal(org.mockito.ArgumentMatchers.eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Meta nao encontrada para id=99"));

        String payload = """
                {
                  "accountId": 1,
                  "name": "Notebook",
                  "targetAmount": 3200.00
                }
                """;

        mockMvc.perform(put("/api/v1/goals/99")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Meta nao encontrada para id=99"));
    }

    private GoalItemResult goalResult(Long goalId, Long accountId, String name, String targetAmount, boolean active) {
        return new GoalItemResult(
                goalId,
                accountId,
                name,
                new BigDecimal(targetAmount),
                active,
                Instant.parse("2026-02-27T12:00:00Z"),
                Instant.parse("2026-02-27T12:00:00Z")
        );
    }
}

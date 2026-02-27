package br.com.autevia.finkidsapi.web;

import br.com.autevia.finkidsapi.service.BonusRuleService;
import br.com.autevia.finkidsapi.service.dto.bonus.BonusRuleResult;
import br.com.autevia.finkidsapi.service.dto.bonus.UpdateBonusRuleCommand;
import br.com.autevia.finkidsapi.web.dto.bonus.BonusRuleResponse;
import br.com.autevia.finkidsapi.web.dto.bonus.UpdateBonusRuleRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
@Validated
public class BonusRuleController {

    private final BonusRuleService bonusRuleService;

    public BonusRuleController(BonusRuleService bonusRuleService) {
        this.bonusRuleService = bonusRuleService;
    }

    @GetMapping("/{accountId}/bonus-rule")
    public BonusRuleResponse getRule(
            @PathVariable @Positive(message = "accountId deve ser maior que zero.") Long accountId
    ) {
        BonusRuleResult result = bonusRuleService.getRule(accountId);
        return toResponse(result);
    }

    @PutMapping("/{accountId}/bonus-rule")
    public BonusRuleResponse upsertRule(
            @PathVariable @Positive(message = "accountId deve ser maior que zero.") Long accountId,
            @Valid @RequestBody UpdateBonusRuleRequest request
    ) {
        BonusRuleResult result = bonusRuleService.upsertRule(
                accountId,
                new UpdateBonusRuleCommand(
                        request.percentage(),
                        request.conditionType(),
                        request.baseType(),
                        request.active()
                )
        );

        return toResponse(result);
    }

    private BonusRuleResponse toResponse(BonusRuleResult result) {
        return new BonusRuleResponse(
                result.bonusRuleId(),
                result.accountId(),
                result.percentage(),
                result.conditionType(),
                result.baseType(),
                result.active(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}

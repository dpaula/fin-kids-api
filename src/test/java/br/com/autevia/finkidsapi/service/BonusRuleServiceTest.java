package br.com.autevia.finkidsapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.autevia.finkidsapi.domain.entity.Account;
import br.com.autevia.finkidsapi.domain.entity.BonusRule;
import br.com.autevia.finkidsapi.domain.enums.BonusBaseType;
import br.com.autevia.finkidsapi.domain.enums.BonusConditionType;
import br.com.autevia.finkidsapi.domain.exception.ResourceNotFoundException;
import br.com.autevia.finkidsapi.domain.exception.ValidationException;
import br.com.autevia.finkidsapi.repository.AccountRepository;
import br.com.autevia.finkidsapi.repository.BonusRuleRepository;
import br.com.autevia.finkidsapi.service.dto.bonus.BonusRuleResult;
import br.com.autevia.finkidsapi.service.dto.bonus.UpdateBonusRuleCommand;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BonusRuleServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private BonusRuleRepository bonusRuleRepository;

    private BonusRuleService bonusRuleService;

    @BeforeEach
    void setUp() {
        bonusRuleService = new BonusRuleService(accountRepository, bonusRuleRepository);
    }

    @Test
    void shouldGetBonusRuleByAccount() {
        Account account = account(1L);
        BonusRule rule = bonusRule(10L, account, "5.00", true);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(bonusRuleRepository.findByAccountId(1L)).thenReturn(Optional.of(rule));

        BonusRuleResult result = bonusRuleService.getRule(1L);

        assertThat(result.bonusRuleId()).isEqualTo(10L);
        assertThat(result.accountId()).isEqualTo(1L);
        assertThat(result.percentage()).isEqualByComparingTo("5.00");
        assertThat(result.conditionType()).isEqualTo(BonusConditionType.NO_WITHDRAWALS_IN_MONTH);
        assertThat(result.baseType()).isEqualTo(BonusBaseType.MONTHLY_DEPOSITS);
        assertThat(result.active()).isTrue();
    }

    @Test
    void shouldCreateBonusRuleWhenItDoesNotExist() {
        Account account = account(1L);
        UpdateBonusRuleCommand command = new UpdateBonusRuleCommand(
                new BigDecimal("10.00"),
                BonusConditionType.NO_WITHDRAWALS_IN_MONTH,
                BonusBaseType.LAST_BALANCE,
                true
        );

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(bonusRuleRepository.findByAccountId(1L)).thenReturn(Optional.empty());
        when(bonusRuleRepository.save(any(BonusRule.class))).thenAnswer(invocation -> {
            BonusRule saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 21L);
            ReflectionTestUtils.setField(saved, "createdAt", Instant.parse("2026-02-27T14:00:00Z"));
            ReflectionTestUtils.setField(saved, "updatedAt", Instant.parse("2026-02-27T14:00:00Z"));
            return saved;
        });

        BonusRuleResult result = bonusRuleService.upsertRule(1L, command);

        assertThat(result.bonusRuleId()).isEqualTo(21L);
        assertThat(result.accountId()).isEqualTo(1L);
        assertThat(result.percentage()).isEqualByComparingTo("10.00");
        assertThat(result.baseType()).isEqualTo(BonusBaseType.LAST_BALANCE);
        assertThat(result.active()).isTrue();
    }

    @Test
    void shouldUpdateBonusRuleWhenItAlreadyExists() {
        Account account = account(1L);
        BonusRule existing = bonusRule(10L, account, "5.00", true);

        UpdateBonusRuleCommand command = new UpdateBonusRuleCommand(
                new BigDecimal("8.50"),
                BonusConditionType.NO_WITHDRAWALS_IN_MONTH,
                BonusBaseType.MONTHLY_DEPOSITS,
                false
        );

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(bonusRuleRepository.findByAccountId(1L)).thenReturn(Optional.of(existing));
        when(bonusRuleRepository.save(existing)).thenReturn(existing);

        BonusRuleResult result = bonusRuleService.upsertRule(1L, command);

        assertThat(result.bonusRuleId()).isEqualTo(10L);
        assertThat(result.percentage()).isEqualByComparingTo("8.50");
        assertThat(result.baseType()).isEqualTo(BonusBaseType.MONTHLY_DEPOSITS);
        assertThat(result.active()).isFalse();
    }

    @Test
    void shouldRejectPercentageOutOfRange() {
        UpdateBonusRuleCommand command = new UpdateBonusRuleCommand(
                new BigDecimal("0.00"),
                BonusConditionType.NO_WITHDRAWALS_IN_MONTH,
                BonusBaseType.LAST_BALANCE,
                true
        );

        assertThatThrownBy(() -> bonusRuleService.upsertRule(1L, command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("percentage");
    }

    @Test
    void shouldThrowNotFoundWhenRuleDoesNotExist() {
        Account account = account(1L);

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(bonusRuleRepository.findByAccountId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bonusRuleService.getRule(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Regra de bonus");
    }

    @Test
    void shouldRejectMissingConditionType() {
        UpdateBonusRuleCommand command = new UpdateBonusRuleCommand(
                new BigDecimal("5.00"),
                null,
                BonusBaseType.LAST_BALANCE,
                true
        );

        assertThatThrownBy(() -> bonusRuleService.upsertRule(1L, command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("conditionType");
    }

    @Test
    void shouldRejectMissingActiveFlag() {
        UpdateBonusRuleCommand command = new UpdateBonusRuleCommand(
                new BigDecimal("5.00"),
                BonusConditionType.NO_WITHDRAWALS_IN_MONTH,
                BonusBaseType.LAST_BALANCE,
                null
        );

        assertThatThrownBy(() -> bonusRuleService.upsertRule(1L, command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("active");
    }

    @Test
    void shouldRejectNullCommand() {
        assertThatThrownBy(() -> bonusRuleService.upsertRule(1L, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Dados da regra de bonus");
    }

    @Test
    void shouldRejectMissingBaseType() {
        UpdateBonusRuleCommand command = new UpdateBonusRuleCommand(
                new BigDecimal("5.00"),
                BonusConditionType.NO_WITHDRAWALS_IN_MONTH,
                null,
                true
        );

        assertThatThrownBy(() -> bonusRuleService.upsertRule(1L, command))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("baseType");
    }

    private Account account(Long id) {
        Account account = new Account("Lucas", "BRL");
        ReflectionTestUtils.setField(account, "id", id);
        return account;
    }

    private BonusRule bonusRule(Long id, Account account, String percentage, boolean active) {
        BonusRule rule = new BonusRule(
                account,
                new BigDecimal(percentage),
                BonusConditionType.NO_WITHDRAWALS_IN_MONTH,
                BonusBaseType.MONTHLY_DEPOSITS,
                active
        );
        ReflectionTestUtils.setField(rule, "id", id);
        ReflectionTestUtils.setField(rule, "createdAt", Instant.parse("2026-02-27T12:00:00Z"));
        ReflectionTestUtils.setField(rule, "updatedAt", Instant.parse("2026-02-27T12:00:00Z"));
        return rule;
    }
}

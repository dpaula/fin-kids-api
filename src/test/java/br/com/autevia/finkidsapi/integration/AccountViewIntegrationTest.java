package br.com.autevia.finkidsapi.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.autevia.finkidsapi.domain.entity.Account;
import br.com.autevia.finkidsapi.domain.entity.AccountTransaction;
import br.com.autevia.finkidsapi.domain.entity.AccountUser;
import br.com.autevia.finkidsapi.domain.entity.AppUser;
import br.com.autevia.finkidsapi.domain.entity.BonusRule;
import br.com.autevia.finkidsapi.domain.entity.Goal;
import br.com.autevia.finkidsapi.domain.enums.BonusBaseType;
import br.com.autevia.finkidsapi.domain.enums.BonusConditionType;
import br.com.autevia.finkidsapi.domain.enums.TransactionOrigin;
import br.com.autevia.finkidsapi.domain.enums.TransactionType;
import br.com.autevia.finkidsapi.domain.enums.UserRole;
import br.com.autevia.finkidsapi.repository.AccountRepository;
import br.com.autevia.finkidsapi.repository.AccountTransactionRepository;
import br.com.autevia.finkidsapi.repository.AccountUserRepository;
import br.com.autevia.finkidsapi.repository.AppUserRepository;
import br.com.autevia.finkidsapi.repository.BonusRuleRepository;
import br.com.autevia.finkidsapi.repository.GoalRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AccountViewIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountTransactionRepository accountTransactionRepository;

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private BonusRuleRepository bonusRuleRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private AccountUserRepository accountUserRepository;

    @Test
    void shouldReturnChildViewWithGoalProgress() throws Exception {
        Account account = accountRepository.save(new Account("Luca", "BRL"));
        String childEmail = "child.view@test.com";
        linkAccountUser(account, childEmail, UserRole.CHILD);

        goalRepository.save(new Goal(account, "Bicicleta", new BigDecimal("200.00"), true));
        goalRepository.save(new Goal(account, "Video Game", new BigDecimal("80.00"), true));

        accountTransactionRepository.save(new AccountTransaction(
                account,
                TransactionType.DEPOSIT,
                TransactionOrigin.MANUAL,
                new BigDecimal("150.00"),
                "Mesada",
                null,
                Instant.parse("2026-03-05T10:00:00Z")
        ));
        accountTransactionRepository.save(new AccountTransaction(
                account,
                TransactionType.WITHDRAW,
                TransactionOrigin.WHATSAPP,
                new BigDecimal("20.00"),
                "Lanche",
                "wa-1",
                Instant.parse("2026-03-06T10:00:00Z")
        ));

        mockMvc.perform(get("/api/v1/accounts/{accountId}/child-view", account.getId())
                        .param("recentTransactionsLimit", "5")
                        .with(jwt().jwt(jwt -> jwt.claim("email", childEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(account.getId()))
                .andExpect(jsonPath("$.currentBalance").value(130.00))
                .andExpect(jsonPath("$.goals.length()").value(2))
                .andExpect(jsonPath("$.goals[0].progressAmount").value(130.00))
                .andExpect(jsonPath("$.goals[0].progressPercent").value(65.00))
                .andExpect(jsonPath("$.recentTransactions.length()").value(2));
    }

    @Test
    void shouldReturnParentViewWithMonthlySummaryAndBonusRule() throws Exception {
        Account account = accountRepository.save(new Account("Nina", "BRL"));
        String parentEmail = "parent.view@test.com";
        linkAccountUser(account, parentEmail, UserRole.PARENT);

        goalRepository.save(new Goal(account, "Patins", new BigDecimal("150.00"), true));
        bonusRuleRepository.save(new BonusRule(
                account,
                new BigDecimal("5.00"),
                BonusConditionType.NO_WITHDRAWALS_IN_MONTH,
                BonusBaseType.MONTHLY_DEPOSITS,
                true
        ));

        accountTransactionRepository.save(new AccountTransaction(
                account,
                TransactionType.DEPOSIT,
                TransactionOrigin.MANUAL,
                new BigDecimal("100.00"),
                "Mesada",
                null,
                Instant.parse("2026-03-02T10:00:00Z")
        ));
        accountTransactionRepository.save(new AccountTransaction(
                account,
                TransactionType.WITHDRAW,
                TransactionOrigin.WHATSAPP,
                new BigDecimal("40.00"),
                "Compra",
                "wa-2",
                Instant.parse("2026-03-08T10:00:00Z")
        ));
        accountTransactionRepository.save(new AccountTransaction(
                account,
                TransactionType.DEPOSIT,
                TransactionOrigin.BONUS,
                new BigDecimal("10.00"),
                "Bonus",
                "bonus:2026-03",
                Instant.parse("2026-03-15T10:00:00Z")
        ));

        mockMvc.perform(get("/api/v1/accounts/{accountId}/parent-view", account.getId())
                        .param("year", "2026")
                        .param("month", "3")
                        .param("recentTransactionsLimit", "2")
                        .with(jwt().jwt(jwt -> jwt.claim("email", parentEmail))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(account.getId()))
                .andExpect(jsonPath("$.currentBalance").value(70.00))
                .andExpect(jsonPath("$.monthlySummary.year").value(2026))
                .andExpect(jsonPath("$.monthlySummary.totalDeposits").value(110.00))
                .andExpect(jsonPath("$.monthlySummary.totalWithdrawals").value(40.00))
                .andExpect(jsonPath("$.bonusRule.percentage").value(5.00))
                .andExpect(jsonPath("$.goals[0].progressPercent").value(46.67))
                .andExpect(jsonPath("$.recentTransactions.length()").value(2));
    }

    @Test
    void shouldReturnUnauthorizedWhenJwtIsMissing() throws Exception {
        Account account = accountRepository.save(new Account("Nina", "BRL"));

        mockMvc.perform(get("/api/v1/accounts/{accountId}/child-view", account.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnForbiddenWhenAccountIsNotLinked() throws Exception {
        Account account = accountRepository.save(new Account("Nina", "BRL"));
        String outsiderEmail = "outsider@test.com";
        appUserRepository.save(new AppUser("Outsider", outsiderEmail, "google-outsider", UserRole.PARENT));

        mockMvc.perform(get("/api/v1/accounts/{accountId}/parent-view", account.getId())
                        .param("year", "2026")
                        .param("month", "3")
                        .with(jwt().jwt(jwt -> jwt.claim("email", outsiderEmail))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Acesso negado para este recurso."));
    }

    private void linkAccountUser(Account account, String email, UserRole role) {
        AppUser user = appUserRepository.save(new AppUser("User " + role.name(), email, "google-" + email, role));
        accountUserRepository.save(new AccountUser(account, user, role));
    }
}

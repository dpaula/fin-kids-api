package br.com.autevia.finkidsapi.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("test")
class DatabaseMigrationCleanSchemaIntegrationTest {

    private static final String DB_NAME = "fin_kids_migration_" + System.nanoTime();

    @DynamicPropertySource
    static void overrideDatasource(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.datasource.url",
                () -> "jdbc:tc:mysql:8.4.0:///" + DB_NAME + "?TC_DAEMON=false"
        );
        registry.add("spring.datasource.driver-class-name", () -> "org.testcontainers.jdbc.ContainerDatabaseDriver");
        registry.add("spring.datasource.username", () -> "test");
        registry.add("spring.datasource.password", () -> "test");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldApplyFullMigrationFromScratchAndCreateIntegrityArtifacts() {
        Integer changeSetCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM DATABASECHANGELOG", Integer.class);
        Integer domainTableCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name IN ('accounts', 'app_users', 'account_users', 'transactions', 'bonus_rules', 'goals', 'audit_events')
                """,
                Integer.class
        );
        Integer checkConstraintCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.table_constraints
                WHERE constraint_schema = DATABASE()
                  AND constraint_type = 'CHECK'
                  AND constraint_name IN (
                    'ck_transactions_amount_positive',
                    'ck_goals_target_amount_positive',
                    'ck_bonus_rules_percentage_range'
                  )
                """,
                Integer.class
        );
        Integer summaryIndexCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(DISTINCT index_name)
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = 'transactions'
                  AND index_name IN (
                    'idx_transactions_account_occurred_type',
                    'idx_transactions_account_occurred_origin'
                  )
                """,
                Integer.class
        );

        assertThat(changeSetCount).isEqualTo(5);
        assertThat(domainTableCount).isEqualTo(7);
        assertThat(checkConstraintCount).isEqualTo(3);
        assertThat(summaryIndexCount).isEqualTo(2);
    }
}

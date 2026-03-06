package br.com.autevia.finkidsapi.repository;

import br.com.autevia.finkidsapi.domain.entity.AccountTransaction;
import br.com.autevia.finkidsapi.domain.enums.TransactionOrigin;
import br.com.autevia.finkidsapi.domain.enums.TransactionType;
import br.com.autevia.finkidsapi.repository.projection.TransactionOriginTotalProjection;
import br.com.autevia.finkidsapi.repository.projection.TransactionTypeTotalProjection;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, Long> {

    boolean existsByAccount_IdAndOriginAndEvidenceReference(
            Long accountId,
            TransactionOrigin origin,
            String evidenceReference
    );

    List<AccountTransaction> findByAccountIdAndOccurredAtBetweenOrderByOccurredAtDesc(
            Long accountId,
            Instant start,
            Instant end
    );

    @Query(
            value = """
                    SELECT COALESCE(SUM(CASE WHEN type = 'DEPOSIT' THEN amount ELSE -amount END), 0)
                    FROM transactions
                    WHERE account_id = :accountId
                    """,
            nativeQuery = true
    )
    BigDecimal calculateBalanceByAccountId(@Param("accountId") Long accountId);

    @Query(
            value = """
                    SELECT COALESCE(SUM(CASE WHEN type = 'DEPOSIT' THEN amount ELSE -amount END), 0)
                    FROM transactions
                    WHERE account_id = :accountId
                      AND occurred_at < :endExclusive
                    """,
            nativeQuery = true
    )
    BigDecimal calculateBalanceByAccountIdBefore(
            @Param("accountId") Long accountId,
            @Param("endExclusive") Instant endExclusive
    );

    @Query(
            """
            SELECT t.type AS type, COALESCE(SUM(t.amount), 0) AS total
            FROM AccountTransaction t
            WHERE t.account.id = :accountId
              AND t.occurredAt >= :start
              AND t.occurredAt < :end
            GROUP BY t.type
            """
    )
    List<TransactionTypeTotalProjection> summarizeMonthlyByType(
            @Param("accountId") Long accountId,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    @Query(
            """
            SELECT t.origin AS origin, COALESCE(SUM(t.amount), 0) AS total
            FROM AccountTransaction t
            WHERE t.account.id = :accountId
              AND t.occurredAt >= :start
              AND t.occurredAt < :end
            GROUP BY t.origin
            """
    )
    List<TransactionOriginTotalProjection> summarizeMonthlyByOrigin(
            @Param("accountId") Long accountId,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    boolean existsByAccountIdAndTypeAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
            Long accountId,
            TransactionType type,
            Instant startInclusive,
            Instant endExclusive
    );

    AccountTransaction findTopByAccountIdAndTypeAndOriginNotAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtDesc(
            Long accountId,
            TransactionType type,
            TransactionOrigin excludedOrigin,
            Instant startInclusive,
            Instant endExclusive
    );

    @Query(
            """
            SELECT COALESCE(SUM(t.amount), 0)
            FROM AccountTransaction t
            WHERE t.account.id = :accountId
              AND t.type = :type
              AND t.occurredAt >= :start
              AND t.occurredAt < :end
            """
    )
    BigDecimal sumAmountByAccountIdAndTypeAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
            @Param("accountId") Long accountId,
            @Param("type") TransactionType type,
            @Param("start") Instant start,
            @Param("end") Instant end
    );
}

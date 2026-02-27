package br.com.autevia.finkidsapi.repository;

import br.com.autevia.finkidsapi.domain.entity.AccountTransaction;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, Long> {

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
}

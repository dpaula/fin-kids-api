package br.com.autevia.finkidsapi.repository;

import br.com.autevia.finkidsapi.domain.entity.AccountTransaction;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, Long> {

    List<AccountTransaction> findByAccountIdAndOccurredAtBetweenOrderByOccurredAtDesc(
            Long accountId,
            Instant start,
            Instant end
    );
}

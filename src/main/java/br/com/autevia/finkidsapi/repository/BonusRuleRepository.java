package br.com.autevia.finkidsapi.repository;

import br.com.autevia.finkidsapi.domain.entity.BonusRule;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BonusRuleRepository extends JpaRepository<BonusRule, Long> {

    Optional<BonusRule> findByAccountId(Long accountId);
}

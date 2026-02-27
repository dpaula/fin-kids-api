package br.com.autevia.finkidsapi.repository;

import br.com.autevia.finkidsapi.domain.entity.Goal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalRepository extends JpaRepository<Goal, Long> {

    List<Goal> findByAccountIdAndActiveTrue(Long accountId);

    Optional<Goal> findByIdAndAccountIdAndActiveTrue(Long goalId, Long accountId);
}

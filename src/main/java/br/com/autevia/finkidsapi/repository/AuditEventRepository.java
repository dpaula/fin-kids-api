package br.com.autevia.finkidsapi.repository;

import br.com.autevia.finkidsapi.domain.entity.AuditEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    List<AuditEvent> findByAccountIdOrderByCreatedAtAsc(Long accountId);
}

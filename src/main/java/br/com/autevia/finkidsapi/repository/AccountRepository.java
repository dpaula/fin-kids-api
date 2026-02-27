package br.com.autevia.finkidsapi.repository;

import br.com.autevia.finkidsapi.domain.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
}

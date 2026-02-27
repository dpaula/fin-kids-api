package br.com.autevia.finkidsapi.repository;

import br.com.autevia.finkidsapi.domain.entity.AccountUser;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountUserRepository extends JpaRepository<AccountUser, Long> {

    List<AccountUser> findByAccountId(Long accountId);
}

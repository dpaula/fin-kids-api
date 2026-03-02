package br.com.autevia.finkidsapi.repository;

import br.com.autevia.finkidsapi.domain.entity.AccountUser;
import br.com.autevia.finkidsapi.domain.enums.UserRole;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountUserRepository extends JpaRepository<AccountUser, Long> {

    List<AccountUser> findByAccountId(Long accountId);

    Optional<AccountUser> findByAccount_IdAndUser_Id(Long accountId, Long userId);

    boolean existsByAccount_IdAndUser_EmailAndProfileRoleIn(
            Long accountId,
            String email,
            Collection<UserRole> roles
    );

    @EntityGraph(attributePaths = {"account", "user"})
    List<AccountUser> findByUser_EmailOrderByAccount_IdAsc(String email);

    @EntityGraph(attributePaths = {"user"})
    List<AccountUser> findByAccount_IdOrderByIdAsc(Long accountId);
}

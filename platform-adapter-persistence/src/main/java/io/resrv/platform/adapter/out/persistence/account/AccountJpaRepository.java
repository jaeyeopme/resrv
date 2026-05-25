package io.resrv.platform.adapter.out.persistence.account;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

interface AccountJpaRepository extends CrudRepository<AccountJpaEntity, UUID> {

    Optional<AccountJpaEntity> findByEmail(String email);
}

package io.resrv.platform.adapter.out.persistence.account;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

interface AccountJpaRepository extends CrudRepository<AccountJpaEntity, UUID> {

    Optional<AccountJpaEntity> findByEmail(String email);

    @Modifying
    @Query("UPDATE AccountJpaEntity a SET a.hashedPassword = :hashedPassword WHERE a.id = :id")
    int updatePasswordHash(@Param("id") UUID id, @Param("hashedPassword") String hashedPassword);
}

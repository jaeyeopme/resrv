package io.resrv.platform.adapter.out.persistence.account;

import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

interface SignInAttemptJpaRepository extends CrudRepository<SignInAttemptJpaEntity, UUID> {}

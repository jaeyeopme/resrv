package io.resrv.ticketing.adapter.out.persistence.purchase;

import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

interface TicketPurchaseJpaRepository extends CrudRepository<TicketPurchaseJpaEntity, UUID> {

    List<TicketPurchaseJpaEntity> findAllByTicketEventIdAndCustomerAccountId(
            UUID ticketEventId, UUID customerAccountId);
}

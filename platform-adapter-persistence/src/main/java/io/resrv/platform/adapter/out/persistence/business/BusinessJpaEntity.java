package io.resrv.platform.adapter.out.persistence.business;

import io.resrv.platform.domain.business.Business;
import io.resrv.platform.domain.business.BusinessName;
import io.resrv.platform.domain.business.BusinessSlug;
import io.resrv.platform.domain.business.BusinessStatus;
import io.resrv.shared.kernel.BusinessId;
import io.resrv.shared.kernel.Timezone;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "platform", name = "business")
class BusinessJpaEntity {

    @Id private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 63)
    private String slug;

    @Column(nullable = false, length = 64)
    private String timezone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BusinessStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected BusinessJpaEntity() {}

    BusinessJpaEntity(
            final UUID id,
            final String name,
            final String slug,
            final String timezone,
            final BusinessStatus status,
            final Instant createdAt) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.timezone = timezone;
        this.status = status;
        this.createdAt = createdAt;
    }

    static BusinessJpaEntity fromDomain(final Business business) {
        return new BusinessJpaEntity(
                business.id().value(),
                business.name().value(),
                business.slug().value(),
                business.timezone().value().getId(),
                business.status(),
                business.createdAt());
    }

    Business toDomain() {
        return Business.reconstitute(
                BusinessId.of(id),
                new BusinessName(name),
                new BusinessSlug(slug),
                Timezone.of(timezone),
                status,
                createdAt);
    }
}

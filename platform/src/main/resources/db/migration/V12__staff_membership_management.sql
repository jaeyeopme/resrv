ALTER TABLE platform.business_membership
    ADD COLUMN updated_at TIMESTAMPTZ,
    ADD COLUMN disabled_at TIMESTAMPTZ;

UPDATE platform.business_membership
SET updated_at = created_at
WHERE updated_at IS NULL;

ALTER TABLE platform.business_membership
    ALTER COLUMN updated_at SET NOT NULL,
    ADD CONSTRAINT ck_platform_business_membership_disabled_at CHECK (
        (active = true AND disabled_at IS NULL)
        OR (active = false AND disabled_at IS NOT NULL)
    ),
    ADD CONSTRAINT ck_platform_business_membership_updated_at CHECK (updated_at >= created_at);

CREATE INDEX idx_platform_business_membership_business_role_active
    ON platform.business_membership(business_id, role, active);

CREATE TABLE platform.business_membership_audit_entry (
    id UUID PRIMARY KEY,
    membership_id UUID NOT NULL,
    business_id UUID NOT NULL,
    actor_account_id UUID NOT NULL,
    target_account_id UUID NOT NULL,
    action VARCHAR(64) NOT NULL,
    previous_role VARCHAR(32),
    previous_active BOOLEAN,
    new_role VARCHAR(32),
    new_active BOOLEAN,
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_platform_business_membership_audit_membership
        FOREIGN KEY (membership_id) REFERENCES platform.business_membership(id),
    CONSTRAINT fk_platform_business_membership_audit_business
        FOREIGN KEY (business_id) REFERENCES platform.business(id),
    CONSTRAINT fk_platform_business_membership_audit_actor
        FOREIGN KEY (actor_account_id) REFERENCES platform.account(id),
    CONSTRAINT fk_platform_business_membership_audit_target
        FOREIGN KEY (target_account_id) REFERENCES platform.account(id),
    CONSTRAINT ck_platform_business_membership_audit_action CHECK (
        action IN ('GRANTED', 'ROLE_CHANGED', 'REACTIVATED', 'DISABLED')
    ),
    CONSTRAINT ck_platform_business_membership_audit_previous_role CHECK (
        previous_role IS NULL OR previous_role IN ('OWNER', 'STAFF')
    ),
    CONSTRAINT ck_platform_business_membership_audit_new_role CHECK (
        new_role IS NULL OR new_role IN ('OWNER', 'STAFF')
    )
);

CREATE INDEX idx_platform_business_membership_audit_business_occurred
    ON platform.business_membership_audit_entry(business_id, occurred_at DESC);

CREATE INDEX idx_platform_business_membership_audit_membership_occurred
    ON platform.business_membership_audit_entry(membership_id, occurred_at DESC);

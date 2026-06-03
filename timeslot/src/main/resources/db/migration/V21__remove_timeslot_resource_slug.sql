ALTER TABLE timeslot.resource
    DROP CONSTRAINT IF EXISTS uq_timeslot_resource_business_slug;

ALTER TABLE timeslot.resource
    DROP CONSTRAINT IF EXISTS ck_timeslot_resource_slug_not_blank;

ALTER TABLE timeslot.resource
    DROP COLUMN IF EXISTS slug;

# Glossary

| Term | Definition |
|---|---|
| Account | Platform identity used by owners, staff, and customers |
| Account-scoped JWT | Token that identifies only the platform account and carries no business role or authority |
| Business | Organization that owns booking configuration, resources, schedules, and reservations |
| BusinessMembership | Relationship granting an account `OWNER` or `STAFF` access to a business |
| BusinessMembershipAuditEntry | Append-only record of membership grant, reactivation, role-change, or disablement |
| Owner | Account with `OWNER` membership for a business |
| Staff | Account with `STAFF` membership for a business |
| Customer | Account that owns a reservation from the booking side |
| Booking settings | Business defaults for slot duration, hold TTL, cancellation window, and max advance booking days |
| Resource | Reservable item within a business |
| Resource override | Resource-level slot duration, hold TTL, or cancellation window override |
| Weekly schedule | Recurring schedule windows for a resource and day of week |
| Date override | Date-specific schedule windows replacing the weekly schedule for that date |
| Schedule window | Local-time open interval with `startTime` and `endTime` |
| Slot | Virtual bookable time range generated from settings and schedule |
| Slot ID | Opaque encoded value binding business, resource, start time, and end time |
| Reservation | Booking record with timestamp facts for lifecycle transitions |
| Sign-in attempt | Security-relevant password sign-in attempt recorded without raw credentials |
| Sign-in protection | Account-scoped state that requires password reset after repeated failed password attempts |
| Password reset challenge | Single-use recovery challenge delivered by email and stored only by token digest |
| Active access decision | Request-time decision based on current account, business, membership, role, or ownership state |
| IDOR | Insecure Direct Object Reference; an access-control risk where a caller probes another user's or business's object id |
| Hold | Temporary reservation before confirmation |
| Active blocker | Confirmed reservation or unexpired hold that blocks overlapping capacity |
| Released | Held reservation released by the customer before confirmation |
| Customer cancelled | Confirmed reservation cancelled by the reservation owner before cutoff |
| Business cancelled | Held or confirmed reservation cancelled by owner/staff |
| Checked in | Confirmed reservation marked attended after start time |
| No-show | Confirmed reservation marked missed after end time |
| Platform context | Bounded context for account, business, and membership |
| Platform exchange | Pure Java platform-owned lookup and decision API surface consumed by other contexts |
| Timeslot context | Bounded context for booking configuration, slots, and reservations |
| Shared kernel | Small set of stable primitives shared across contexts |
| ADR | Architecture Decision Record explaining why a durable technical decision exists |
| PRD | Product Requirements Document |
| TRD | Technical Requirements and Design document |

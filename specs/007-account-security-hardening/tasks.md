# Tasks: Account Security Hardening

**Input**: Design documents from `specs/007-account-security-hardening/`
**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`

**Tests**: Included because this feature changes authentication, authorization, generated API behavior, persistence, and public discovery semantics.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel with other tasks in the same phase when dependencies are met
- **[Story]**: Maps task to a user story from `spec.md`
- Every task includes exact file paths

## Phase 1: Setup

**Purpose**: Add shared dependencies and configuration surface needed by all stories.

- [ ] T001 Add Spring Mail dependency alias in `gradle/libs.versions.toml`
- [ ] T002 Add Spring Mail implementation dependency to `platform/build.gradle.kts`
- [ ] T003 [P] Document local SMTP configuration placeholders in `docs/operations.md`
- [ ] T004 [P] Add email and reset configuration property notes to `docs/security.md`

---

## Phase 2: Foundational

**Purpose**: Core persistence, domain, ports, and error primitives that block all user stories.

**CRITICAL**: No user story implementation should begin until this phase is complete.

- [ ] T005 Create Flyway migration `platform/src/main/resources/db/migration/V11__account_security_hardening.sql` for sign-in attempt, sign-in protection, and password reset challenge tables
- [ ] T006 [P] Add sign-in attempt outcome enum in `platform/src/main/java/io/resrv/platform/domain/account/SignInAttemptOutcome.java`
- [ ] T007 [P] Add account sign-in protection domain type in `platform/src/main/java/io/resrv/platform/domain/account/AccountSignInProtection.java`
- [ ] T008 [P] Add password reset challenge domain type in `platform/src/main/java/io/resrv/platform/domain/account/PasswordResetChallenge.java`
- [ ] T009 [P] Add password reset token value object in `platform/src/main/java/io/resrv/platform/domain/account/PasswordResetToken.java`
- [ ] T010 [P] Add sign-in security ports in `platform/src/main/java/io/resrv/platform/application/auth/out/SignInAttemptCommandPort.java`, `platform/src/main/java/io/resrv/platform/application/auth/out/SignInProtectionCommandPort.java`, and `platform/src/main/java/io/resrv/platform/application/auth/out/SignInProtectionQueryPort.java`
- [ ] T011 [P] Add email delivery port in `platform/src/main/java/io/resrv/platform/application/auth/out/PasswordResetEmailPort.java`
- [ ] T012 [P] Add reset token generator/hasher ports in `platform/src/main/java/io/resrv/platform/application/security/out/PasswordResetTokenGeneratorPort.java` and `platform/src/main/java/io/resrv/platform/application/security/out/PasswordResetTokenHashingPort.java`
- [ ] T013 [P] Add account active lookup contract in `platform/src/main/java/io/resrv/platform/contract/account/ActiveAccountCheck.java`
- [ ] T014 Update `platform/src/main/java/io/resrv/platform/contract/PlatformLookupContractConfiguration.java` to export active account checking
- [ ] T015 Add JPA entities and repositories for auth security state in `platform/src/main/java/io/resrv/platform/adapter/out/persistence/account/SignInAttemptJpaEntity.java`, `platform/src/main/java/io/resrv/platform/adapter/out/persistence/account/SignInAttemptJpaRepository.java`, `platform/src/main/java/io/resrv/platform/adapter/out/persistence/account/AccountSignInProtectionJpaEntity.java`, `platform/src/main/java/io/resrv/platform/adapter/out/persistence/account/AccountSignInProtectionJpaRepository.java`, `platform/src/main/java/io/resrv/platform/adapter/out/persistence/account/PasswordResetChallengeJpaEntity.java`, and `platform/src/main/java/io/resrv/platform/adapter/out/persistence/account/PasswordResetChallengeJpaRepository.java`
- [ ] T016 Add persistence adapter for sign-in protection and reset challenges in `platform/src/main/java/io/resrv/platform/adapter/out/persistence/account/AccountSecurityPersistenceAdapter.java`
- [ ] T017 Add persistence tests for auth security schema and adapters in `platform/src/test/java/io/resrv/platform/adapter/out/persistence/account/AccountSecurityPersistenceAdapterTest.java`
- [ ] T018 Add reusable auth security exceptions in `platform/src/main/java/io/resrv/platform/application/auth/PasswordResetRequiredException.java` and `platform/src/main/java/io/resrv/platform/application/auth/PasswordResetTokenInvalidException.java`
- [ ] T019 Update platform problem responses for auth security exceptions in `platform/src/main/java/io/resrv/platform/adapter/in/web/error/PlatformExceptionHandler.java`

**Checkpoint**: Foundation compiles, migration is covered, and story work can begin.

---

## Phase 3: User Story 1 - Slow Repeated Failed Sign-In Attempts (Priority: P1) MVP

**Goal**: Five failed password attempts for an existing account trigger a reset email without revealing account existence.

**Independent Test**: Repeated failed sign-ins are accepted as generic failures until the fifth failure, and the fifth failure produces one reset email for the registered address.

### Tests for User Story 1

- [ ] T020 [P] [US1] Add login failure counting service tests in `platform/src/test/java/io/resrv/platform/application/auth/LoginServiceTest.java`
- [ ] T021 [P] [US1] Add non-enumerating login API integration tests in `platform/src/test/java/io/resrv/platform/api/PlatformApiIntegrationTest.java`
- [ ] T022 [US1] Add email delivery fake assertions in `platform/src/test/java/io/resrv/platform/api/PlatformApiIntegrationTest.java`

### Implementation for User Story 1

- [ ] T023 [P] [US1] Add SMTP password reset email adapter in `platform/src/main/java/io/resrv/platform/adapter/out/email/SmtpPasswordResetEmailAdapter.java`
- [ ] T024 [P] [US1] Add test fake password reset email adapter in `platform/src/test/java/io/resrv/platform/api/FakePasswordResetEmailAdapter.java`
- [ ] T025 [P] [US1] Add reset token generator and digest adapter in `platform/src/main/java/io/resrv/platform/api/security/PasswordResetTokenAdapter.java`
- [ ] T026 [US1] Update `platform/src/main/java/io/resrv/platform/application/auth/LoginService.java` to record failed attempts, create reset challenge on the fifth failure, and send reset email
- [ ] T027 [US1] Update `platform/src/main/java/io/resrv/platform/adapter/in/web/auth/LoginWebAdapter.java` with Springdoc annotations and non-sensitive recovery-required response documentation
- [ ] T028 [US1] Update `platform/src/main/java/io/resrv/platform/api/security/PlatformSecurityConfig.java` to expose email/reset configuration beans needed by login protection

**Checkpoint**: User Story 1 is independently testable with `./gradlew :platform:test`.

---

## Phase 4: User Story 2 - Require Email Verification After Suspicious Failures (Priority: P2)

**Goal**: Accounts requiring password reset cannot sign in with any password until password reset succeeds; unrelated accounts remain unaffected.

**Independent Test**: Trigger 5 failed attempts for one account, verify sign-in remains blocked, reset password through the email link, and verify sign-in succeeds only with the new password.

### Tests for User Story 2

- [ ] T029 [P] [US2] Add password reset application tests in `platform/src/test/java/io/resrv/platform/application/auth/PasswordResetServiceTest.java`
- [ ] T030 [P] [US2] Add password reset API integration tests in `platform/src/test/java/io/resrv/platform/api/PlatformApiIntegrationTest.java`
- [ ] T031 [P] [US2] Add unrelated-account isolation tests in `platform/src/test/java/io/resrv/platform/application/auth/LoginServiceTest.java`

### Implementation for User Story 2

- [ ] T032 [P] [US2] Add reset password use case types in `platform/src/main/java/io/resrv/platform/application/auth/in/ResetPasswordCommand.java`, `platform/src/main/java/io/resrv/platform/application/auth/in/ResetPasswordResult.java`, and `platform/src/main/java/io/resrv/platform/application/auth/in/ResetPasswordUseCase.java`
- [ ] T033 [US2] Implement password reset service in `platform/src/main/java/io/resrv/platform/application/auth/PasswordResetService.java`
- [ ] T034 [US2] Add password reset web endpoint in `platform/src/main/java/io/resrv/platform/adapter/in/web/auth/PasswordResetWebAdapter.java`
- [ ] T035 [US2] Update `platform/src/main/java/io/resrv/platform/application/auth/LoginService.java` to reject sign-in while reset is required
- [ ] T036 [US2] Add account password command port in `platform/src/main/java/io/resrv/platform/application/account/out/AccountPasswordCommandPort.java` and implement password hash persistence in `platform/src/main/java/io/resrv/platform/adapter/out/persistence/account/AccountPersistenceAdapter.java`
- [ ] T037 [US2] Add Springdoc request/response annotations for reset behavior in `platform/src/main/java/io/resrv/platform/adapter/in/web/auth/PasswordResetWebAdapter.java`
- [ ] T038 [US2] Permit the password reset endpoint in `platform/src/main/java/io/resrv/platform/api/security/PlatformSecurityConfig.java`

**Checkpoint**: User Stories 1 and 2 work independently with `./gradlew :platform:test`.

---

## Phase 5: User Story 3 - Recheck Active Access On Protected Actions (Priority: P3)

**Goal**: Protected actions deny inactive accounts, inactive businesses, and inactive owner/staff memberships at request time while public discovery remains reachable and filters inactive bookable results.

**Independent Test**: Disable account, business, or membership in test setup and verify protected actions deny access; public resource/slot endpoints stay reachable and exclude inactive bookable results.

### Tests for User Story 3

- [ ] T039 [P] [US3] Add active account platform API integration tests in `platform/src/test/java/io/resrv/platform/api/PlatformApiIntegrationTest.java`
- [ ] T040 [P] [US3] Add active account contract tests in `platform/src/test/java/io/resrv/platform/application/account/ActiveAccountCheckServiceTest.java`
- [ ] T041 [P] [US3] Add active business/member timeslot integration tests in `timeslot/src/test/java/io/resrv/timeslot/api/TimeslotBookingApiIntegrationTest.java`
- [ ] T042 [P] [US3] Add public discovery inactive business/resource tests in `timeslot/src/test/java/io/resrv/timeslot/application/slot/VirtualSlotServiceTest.java`

### Implementation for User Story 3

- [ ] T043 [P] [US3] Implement active account check service in `platform/src/main/java/io/resrv/platform/application/account/ActiveAccountCheckService.java`
- [ ] T044 [US3] Update `platform/src/main/java/io/resrv/platform/api/security/PlatformSecurityConfig.java` to enforce active account checks on protected authenticated requests
- [ ] T045 [US3] Update `platform/src/main/java/io/resrv/platform/application/membership/CheckBusinessAccessService.java` to require active account, active business, and active owner/staff membership
- [ ] T046 [US3] Document boolean denial semantics for inactive account, inactive business, and inactive membership in `platform/src/main/java/io/resrv/platform/contract/membership/BusinessAccessCheck.java`
- [ ] T047 [US3] Update `timeslot/src/main/java/io/resrv/timeslot/adapter/out/platform/PlatformBusinessLookupAdapter.java` to preserve contract-only platform access
- [ ] T048 [US3] Update `timeslot/src/main/java/io/resrv/timeslot/application/resource/ResourceService.java` to exclude resources when the business is inactive
- [ ] T049 [US3] Update `timeslot/src/main/java/io/resrv/timeslot/application/slot/VirtualSlotService.java` so inactive businesses/resources return no bookable slots through public discovery
- [ ] T050 [US3] Update `timeslot/src/main/java/io/resrv/timeslot/adapter/in/web/resource/ResourceWebAdapter.java` and `timeslot/src/main/java/io/resrv/timeslot/adapter/in/web/slot/SlotWebAdapter.java` Springdoc annotations for public discovery behavior

**Checkpoint**: All user stories are independently functional with `./gradlew :platform:test :timeslot:test`.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Contract visibility, docs consistency, and full verification.

- [ ] T051 [P] Update deferred hardening notes in `docs/security.md`
- [ ] T052 [P] Update known gaps and focused checks in `docs/testing.md`
- [ ] T053 [P] Update SMTP and password reset environment notes in `docs/operations.md`
- [ ] T054 [P] Add or update ADR if password reset/email delivery introduces a durable architecture decision in `docs/adr/`
- [ ] T055 Verify generated OpenAPI behavior manually or through integration assertions in `platform/src/test/java/io/resrv/platform/api/PlatformApiIntegrationTest.java`
- [ ] T056 Run quickstart acceptance checks from `specs/007-account-security-hardening/quickstart.md`
- [ ] T057 Run `./gradlew spotlessApply` from `/Users/jaeyeop/Workspace/resrv`
- [ ] T058 Run `./gradlew rewriteDryRun` from `/Users/jaeyeop/Workspace/resrv`
- [ ] T059 Run `./gradlew check` from `/Users/jaeyeop/Workspace/resrv`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies.
- **Foundational (Phase 2)**: Depends on Setup; blocks all user stories.
- **User Story 1 (Phase 3)**: Depends on Foundational; MVP.
- **User Story 2 (Phase 4)**: Depends on Foundational and integrates with US1 sign-in protection state.
- **User Story 3 (Phase 5)**: Depends on Foundational; may run after or alongside US1/US2 if platform contract changes are coordinated.
- **Polish (Phase 6)**: Depends on selected user stories being complete.

### User Story Dependencies

- **US1**: First recommended slice; proves failure counting, email trigger, and non-enumeration.
- **US2**: Builds on US1 challenge state to complete reset and unblock sign-in.
- **US3**: Independent of reset flow for business value, but shares platform contract/security configuration files.

### Parallel Opportunities

- T003 and T004 can run in parallel after T001/T002 decisions are known.
- T006-T013 can run in parallel after T005 schema shape is agreed.
- T020-T021 can run in parallel before US1 implementation; T022 shares `PlatformApiIntegrationTest.java` and should follow T021.
- T023-T025 can run in parallel before T026.
- T029-T031 can run in parallel before US2 implementation.
- T039-T042 can run in parallel before US3 implementation.
- T051-T054 can run in parallel during polish.

---

## Parallel Example: User Story 1

```text
Task: "T020 [P] [US1] Add login failure counting service tests in platform/src/test/java/io/resrv/platform/application/auth/LoginServiceTest.java"
Task: "T021 [P] [US1] Add non-enumerating login API integration tests in platform/src/test/java/io/resrv/platform/api/PlatformApiIntegrationTest.java"
```

```text
Task: "T023 [P] [US1] Add SMTP password reset email adapter in platform/src/main/java/io/resrv/platform/adapter/out/email/SmtpPasswordResetEmailAdapter.java"
Task: "T024 [P] [US1] Add test fake password reset email adapter in platform/src/test/java/io/resrv/platform/api/FakePasswordResetEmailAdapter.java"
Task: "T025 [P] [US1] Add reset token generator and digest adapter in platform/src/main/java/io/resrv/platform/api/security/PasswordResetTokenAdapter.java"
```

## Parallel Example: User Story 3

```text
Task: "T039 [P] [US3] Add active account platform API integration tests in platform/src/test/java/io/resrv/platform/api/PlatformApiIntegrationTest.java"
Task: "T041 [P] [US3] Add active business/member timeslot integration tests in timeslot/src/test/java/io/resrv/timeslot/api/TimeslotBookingApiIntegrationTest.java"
Task: "T042 [P] [US3] Add public discovery inactive business/resource tests in timeslot/src/test/java/io/resrv/timeslot/application/slot/VirtualSlotServiceTest.java"
```

---

## Implementation Strategy

### MVP First

1. Complete Phase 1 and Phase 2.
2. Complete Phase 3 (US1).
3. Validate with `./gradlew :platform:test`.
4. Stop for review if only MVP sign-in protection is needed.

### Incremental Delivery

1. Add US1 to count failures and send reset email.
2. Add US2 to complete reset and unblock sign-in safely.
3. Add US3 to enforce active-state checks and public discovery filtering.
4. Finish Phase 6 and run full verification.

### Quality Bar

- Tests for each story should fail before implementation.
- Preserve platform/timeslot hexagonal boundaries.
- Do not add business role, business identity, or actor authority to tokens.
- Do not create a handwritten endpoint catalog.
- Commit after each completed phase or cohesive story slice.

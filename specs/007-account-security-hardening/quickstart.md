# Quickstart: Account Security Hardening

## Prerequisites

- JDK 25+
- Docker running for Testcontainers
- Repository dependencies installed when commit hooks are needed:

```bash
npm ci
npm run hooks:install
```

## Focused Verification

Run platform tests while implementing platform auth, reset, email, and active account checks:

```bash
./gradlew :platform:test
```

For local manual email delivery checks, configure SMTP settings for the platform runtime. Automated
tests should use the fake email adapter instead of a network provider.

Run timeslot tests while implementing public discovery and platform contract changes:

```bash
./gradlew :timeslot:test
```

Run architecture checks after changing cross-module contracts:

```bash
./gradlew :platform:check :timeslot:check
```

## End-to-End Acceptance Checks

1. Register an account.
2. Fail password sign-in 5 times for that account.
3. Verify that a reset email is delivered to the registered email address.
4. Verify that password sign-in remains blocked until reset succeeds.
5. Complete password reset from the email link.
6. Verify that sign-in succeeds with the new password.
7. Verify that unrelated accounts and public booking discovery are unaffected.
8. Mark an account, business, or membership inactive in test setup.
9. Verify protected actions deny access according to the latest active state.
10. Verify documentation and public booking discovery endpoints remain reachable.

## Full Verification

Before merging:

```bash
./gradlew spotlessApply
./gradlew rewriteDryRun
./gradlew check
```

## Expected Artifacts

- Platform migration for sign-in protection and reset challenge state.
- Platform auth application tests for failure counting, reset requirement, reset completion, and
  non-enumerating responses.
- Platform API integration tests for sign-in, password reset, active account checks, and generated
  contract visibility.
- Timeslot integration tests for inactive business/resource public discovery behavior and
  owner-account protection isolation.
- ArchUnit remains green for platform/timeslot boundary rules.
